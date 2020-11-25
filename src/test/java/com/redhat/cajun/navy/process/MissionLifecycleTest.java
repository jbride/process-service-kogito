package com.redhat.cajun.navy.process;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.After;
import org.junit.jupiter.api.Test;
import org.kie.kogito.kafka.KafkaClient;
import org.kie.kogito.testcontainers.quarkus.KafkaQuarkusTestResource;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import io.cloudevents.v03.CloudEventBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import com.redhat.cajun.navy.rules.model.Mission;
import com.redhat.cajun.navy.rules.model.Incident;
import com.redhat.cajun.navy.rules.model.Status;

@QuarkusTest
//@QuarkusTestResource(KafkaQuarkusTestResource.class)
public class MissionLifecycleTest {

    private static final String  TOPIC_MISSION_EVENT = "topic-mission-event";
    private static final String TOPIC_INCIDENT_COMMAND = "topic-incident-command";

    private static Logger log = Logger.getLogger(MissionLifecycleTest.class);
    
    @Inject
    private ObjectMapper objectMapper;
    
    public KafkaClient kafkaClient;
    
    @ConfigProperty(name = KafkaQuarkusTestResource.KOGITO_KAFKA_PROPERTY)
    private String kafkaBootstrapServers;

    private long sleepBetweenStateChanges=30000;

    @Test
    public void testProcess() throws InterruptedException, JsonProcessingException {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        kafkaClient = new KafkaClient(kafkaBootstrapServers);
        Mission missionObj = createMission();

        // https://github.com/kiegroup/kogito-examples/blob/stable/process-kafka-quickstart-quarkus/src/test/java/org/acme/travel/MessagingIT.java#L74-L96
        // https://github.com/kiegroup/kogito-apps/blob/master/data-index/data-index-service/data-index-service-common/src/test/java/org/kie/kogito/index/messaging/AbstractReactiveMessagingEventConsumerKafkaIT.java
        // https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2.4/model/model.html#skipping

        kafkaClient.consume(TOPIC_INCIDENT_COMMAND, s -> {
            try {
                Incident iObj = objectMapper.readValue(s, Incident.class);
                log.infov("Received incident with status: {0}", iObj.getStatus());
                assertEquals(iObj.getStatus(), Status.ASSIGNED.name());
            } catch (JsonProcessingException e) {
                log.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });
        sendEvent(missionObj, Status.UNASSIGNED, TOPIC_MISSION_EVENT);

/*
        kafkaClient.consume(I_MISSION_STARTED_TOPIC_CHANNEL, s -> {
            try {
                Mission mObj = objectMapper.readValue(s, Mission.class);
                log.info("Received mission with status: {0}", mObj.getStatus().name());
                assertEquals(mObj.getStatus().name(), Status.STARTED.name());
            } catch (JsonProcessingException e) {
                log.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });
        sendEvent(missionObj, Mission.Status.STARTED, O_INCIDENT_COMMAND_ASSIGNED);

        kafkaClient.consume(I_MISSION_PICKEDUP_TOPIC_CHANNEL, s -> {
            try {
                Mission mObj = objectMapper.readValue(s, Mission.class);
                log.info("Received mission with status: {0}", mObj.getStatus().name());
                assertEquals(mObj.getStatus().name(), Status.PICKEDUP.name());
            } catch (JsonProcessingException e) {
                log.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });
        sendEvent(missionObj, Mission.Status.PICKEDUP, O_INCIDENT_COMMAND_PICKEDUP);

        kafkaClient.consume(I_MISSION_DROPPEDOFF_TOPIC_CHANNEL, s -> {
            try {
                Mission mObj = objectMapper.readValue(s, Mission.class);
                log.info("Received mission with status: {0}", mObj.getStatus().name());
                assertEquals(mObj.getStatus().name(), Status.DROPPED.name());
            } catch (JsonProcessingException e) {
                log.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });
        sendEvent(missionObj, Mission.Status.DROPPED, O_INCIDENT_COMMAND_DELIVERED);
*/
    }
    
    private void sendEvent(Mission missionObj, Status mStatus, String topic) throws JsonProcessingException, InterruptedException {
        missionObj.setStatus(mStatus);
        String mJson = generateEvent(missionObj);
        kafkaClient.produce(mJson, topic);
        log.infov("Sent event to topic: {0}", topic);
        Thread.sleep(sleepBetweenStateChanges);
    }

    private Mission createMission() {
        Mission mObj = new Mission();
        mObj.setIncidentLat(new BigDecimal(0.0));
        mObj.setIncidentLong(new BigDecimal(0.0));
        mObj.setDestinationLat(new BigDecimal(1.0));
        mObj.setDestinationLong(new BigDecimal(1.0));
        mObj.setIncidentId("incident1");
        mObj.setResponderId("responder1");
        mObj.setResponderStartLat(new BigDecimal(2.0));
        mObj.setResponderStartLong(new BigDecimal(2.0));
        mObj.setLastUpdate(System.currentTimeMillis());
        return mObj;
    }

    private String generateEvent(Mission mObj) throws JsonProcessingException {
      return objectMapper.writeValueAsString(mObj);
    }

    @After
    public void stop() {
        Optional.ofNullable(kafkaClient).ifPresent(KafkaClient::shutdown);
    }
}

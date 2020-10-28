package com.redhat.cajun.navy.process;

import com.redhat.cajun.navy.rules.model.Status;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.After;
import org.junit.jupiter.api.Test;
import org.kie.kogito.kafka.KafkaClient;
import org.kie.kogito.testcontainers.quarkus.KafkaQuarkusTestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//import io.cloudevents.v03.CloudEventBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import com.redhat.cajun.navy.rules.model.Mission;

@QuarkusTest
@QuarkusTestResource(KafkaQuarkusTestResource.class)
public class MissionLifecycleTest {

    private static Logger LOGGER = LoggerFactory.getLogger(MissionLifecycleTest.class);
    private static String MISSION_CREATED_TOPIC = "topic-mission-event-created";

    @Inject
    private ObjectMapper objectMapper;

    public KafkaClient kafkaClient;

    @ConfigProperty(name = KafkaQuarkusTestResource.KOGITO_KAFKA_PROPERTY)
    private String kafkaBootstrapServers;

    @Test
    public void testProcess() throws InterruptedException, JsonProcessingException {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        kafkaClient = new KafkaClient(kafkaBootstrapServers);

        //number of generated events to test
        final int count = 1;
        final CountDownLatch countDownLatch = new CountDownLatch(count);

        // https://github.com/kiegroup/kogito-examples/blob/stable/process-kafka-quickstart-quarkus/src/test/java/org/acme/travel/MessagingIT.java#L74-L96
        // https://github.com/kiegroup/kogito-apps/blob/master/data-index/data-index-service/data-index-service-common/src/test/java/org/kie/kogito/index/messaging/AbstractReactiveMessagingEventConsumerKafkaIT.java
        // https://github.com/kiegroup/kogito-examples/blob/stable/process-kafka-quickstart-quarkus/src/test/java/org/acme/travel/MessagingIT.java
        // https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2.4/model/model.html#skipping

        kafkaClient.consume(MISSION_CREATED_TOPIC, s -> {
            LOGGER.info("Received from kafka: {}", s);
            try {
                Mission mObj = objectMapper.readValue(s, Mission.class);
                countDownLatch.countDown();
            } catch (JsonProcessingException e) {
                LOGGER.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });

    
    String mJson = generateEvent(createMission());
	kafkaClient.produce(mJson, MISSION_CREATED_TOPIC);

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertEquals(countDownLatch.getCount(), 0);
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
        mObj.setStatus(Status.REQUESTED);
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

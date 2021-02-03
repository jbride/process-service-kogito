package com.redhat.cajun.navy.process;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.kafka.KafkaClient;
import org.kie.kogito.testcontainers.quarkus.KafkaQuarkusTestResource;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.kie.kogito.services.event.AbstractProcessDataEvent;

import com.redhat.cajun.navy.rules.model.Mission;
import com.redhat.cajun.navy.rules.model.Incident;
import com.redhat.cajun.navy.rules.model.Status;

/*
    Inspired by:  https://github.com/kiegroup/kogito-examples/blob/stable/process-kafka-quickstart-quarkus/src/test/java/org/acme/travel/MessagingIT.java
*/
@QuarkusTest
@QuarkusTestResource(KafkaQuarkusTestResource.class)
public class MissionLifecycleTest {

    private static final String DATA = "data";
    private static final String KOGITO_PROCESS_INSTANCE_ID = "kogitoProcessinstanceId";
    private static final String KOGITO_PROCESS_ID = "kogitoProcessId";
    private static final String KOGITO_PROCESS_INSTANCE_STATE = "kogitoProcessinstanceState";

    private static final String TOPIC_MISSION_EVENT = "topic-mission-event";
    private static final String TOPIC_INCIDENT_COMMAND = "topic-incident-command";
    private static final String MISSION_CREATED = "mission-created";
    private static final String EVACUEE_PICKED_UP = "evacuee-picked-up";
    private static final String EVACUEE_DROPPED_OFF = "evacuee-dropped-off";
    private static final String MISSION_ABORTED = "mission-aborted";
    private static final String INCIDENT_ASSIGNED = "INCIDENT_ASSIGNED";
    private static final String INCIDENT_PICKEDUP = "INCIDENT_PICKEDUP";
    private static final String INCIDENT_DROPPED = "INCIDENT_DROPPED";

    private static Logger log = Logger.getLogger(MissionLifecycleTest.class);

    private static String kogitoProcessInstanceId="";

    private static boolean proceed = false;

    private String expectedIncidentStatus=INCIDENT_ASSIGNED;
    
    @Inject
    private ObjectMapper objectMapper;
    
    public KafkaClient kafkaClient;
    
    @ConfigProperty(name = KafkaQuarkusTestResource.KOGITO_KAFKA_PROPERTY)
    private String kafkaBootstrapServers;

    private long sleepBetweenLockChecks=2000;

    @BeforeEach
    public void setup() {
	/*
            The following jackson configs are critical to ensure CloudEvents are (un)marshalled appropriately
            Otherwise, expect to encounter exceptions such as the following

		com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot construct instance of `com.redhat.cajun.navy.rules.model.Mission` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('eyJpbmNpZGVudElkIjoiaW5jaWRlbnQxIiwicmVzcG9uZGVySWQiOiJyZXNwb25kZXIxIiwic3RhdHVzIjoiVU5BU1NJR05FRCIsInJlc3BvbmRlclN0YXJ0TGF0IjoyLCJyZXNwb25kZXJTdGFydExvbmciOjIsImluY2lkZW50TGF0IjowLCJpbmNpZGVudExvbmciOjAsImRlc3RpbmF0aW9uTGF0IjoxLCJkZXN0aW5hdGlvbkxvbmciOjEsImxhc3RVcGRhdGUiOjE2MTExNzU2NjI3Njd9')
         */
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }

    //@Test
    public void testMissionSerialization() throws IOException {
        Mission missionObj = createMission();
        byte[] missionStringBytes = objectMapper.writeValueAsString(missionObj).getBytes();

        Mission unmarshalledMission = objectMapper.readValue(missionStringBytes, Mission.class);
        log.info("testMissionSerialization() unmarshalledMission = "+unmarshalledMission); 
    }

    //@Test
    public void testMissionAsBytesSerialization() throws IOException {
        Mission missionObj = createMission();
        byte[] missionJsonBytes = objectMapper.writeValueAsString(missionObj).getBytes();  // Get bytes of JSON representation of Mission
        MissionWrapper mWrapper = new MissionWrapper();
        mWrapper.setMissionBytes(missionJsonBytes);
        String mWrapperJson = objectMapper.writeValueAsString(mWrapper);  // Get JSON representation of MissionWrapper

        MissionWrapper unMarshalledMWrapper = objectMapper.readValue(mWrapperJson, MissionWrapper.class);
        log.info("testMissionAsByteSerialization() missionWrapperJsonAsBytes = "+unMarshalledMWrapper.getMissionBytes());

        Mission unMarshalledMission = objectMapper.readValue(unMarshalledMWrapper.getMissionBytes(), Mission.class); // Using byte[] representation of JSON, unmarshall to Mission 
        log.info("testMissionAsByteSerialization() unmarshalledMission = "+unMarshalledMission);

    }

    //@Test
    public void testCloudEventMarshalling() throws IOException {
        try {
            Mission missionObj = createMission();
            String cloudEventJson = generateCloudEventJson(missionObj, MISSION_CREATED);
            log.info("testCloudEventSerialization() mEventJson = "+cloudEventJson); 
    
            MissionLifecycleMessageDataEvent cloudEventObj = objectMapper.readValue(cloudEventJson, MissionLifecycleMessageDataEvent.class);
            
            log.info("testCloudEventSerialization() mission = "+cloudEventObj.getData()); 
        }catch(JsonProcessingException x) {
            x.printStackTrace();
        }
    }

    @Test
    public void testMissionProcess() throws InterruptedException, JsonProcessingException {
        kafkaClient = new KafkaClient(kafkaBootstrapServers);
        Mission missionObj = createMission();

        // Send initial mission event to business process and consume Incident with status of:  Assigned
        kafkaClient.consume(TOPIC_INCIDENT_COMMAND, iJson -> {
            try {
                JsonNode event = objectMapper.readValue(iJson, JsonNode.class);

                /*
                   sample json response:
                       {"id":"11a50afe-8a58-4a0e-8226-4de71a0f744c","source":"/process/missionLifecycle","type":"topic-incident-command","time":"2021-02-03T11:22:02.426166-05:00","data":{"id":null,"latitude":null,"longitude":null,"numPeople":null,"medicalNeeded":null,"reportedTime":null,"status":"INCIDENT_ASSIGNED","reporterId":null},"kogitoProcessinstanceId":"be7a9472-d84b-4f74-bf99-9fbbb6240223","kogitoProcessId":"missionLifecycle","kogitoProcessinstanceState":"1","specversion":"1.0"}

		String json = objectMapper.writeValueAsString(event);
		log.infov("Received json = {0}", json);
                 */

                kogitoProcessInstanceId = event.get(KOGITO_PROCESS_INSTANCE_ID).asText();
                Incident iObj = objectMapper.readValue(event.get(DATA).toString(), Incident.class);
                log.infov("Received incident. pInstanceId: {0}, status: {1}", kogitoProcessInstanceId, iObj.getStatus());
                assertEquals(iObj.getStatus(), expectedIncidentStatus);
            } catch (Throwable e) {
                log.error("Error parsing {}", iJson, e);
                throw new RuntimeException(e);
            }finally {
                proceed = true;
            }
        });
        expectedIncidentStatus=INCIDENT_ASSIGNED;
        sendCloudEvent(missionObj, Status.UNASSIGNED, TOPIC_MISSION_EVENT, MISSION_CREATED );

        expectedIncidentStatus=INCIDENT_PICKEDUP;
        sendCloudEvent(missionObj, Status.REQUESTED, TOPIC_MISSION_EVENT, EVACUEE_PICKED_UP);

        expectedIncidentStatus=INCIDENT_DROPPED;
        sendCloudEvent(missionObj, Status.REQUESTED, TOPIC_MISSION_EVENT, EVACUEE_DROPPED_OFF);
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

    private String generateCloudEventJson(Mission mObj, String messageTrigger) throws JsonProcessingException {

        String jsonMission = objectMapper.writeValueAsString(mObj);

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(""))
            .withType(messageTrigger) // evaluated as "trigger" by org.kie.kogito.event.impl.CloudEventConsumer; correspondes to "message" in intermediate message events
            .withTime(OffsetDateTime.now())
            .withData(jsonMission.getBytes())
            .withExtension("kogitoReferenceId", kogitoProcessInstanceId)
            .build();
        //log.info("generateMissionEvent() cloudEvent = "+cloudEvent.getClass().toString());
      
        return objectMapper.writeValueAsString(cloudEvent);
    }
    
    private void sendCloudEvent(Mission missionObj, Status mStatus, String topic, String messageTrigger) throws JsonProcessingException, InterruptedException {


        // TO-DO: For purpose of current smoke test, this mission status is not evaluated in business process
        missionObj.setStatus(mStatus);

        String mJson = generateCloudEventJson(missionObj, messageTrigger);
        kafkaClient.produce(mJson, topic);
        log.infov("Sent event w/ pInstanceId: {0}, messageTrigger: {1} to topic: {2}", kogitoProcessInstanceId, messageTrigger, topic);
        proceed = false;
        int x = 0;
        while(!proceed){
            if(x < 5) {
                Thread.sleep(sleepBetweenLockChecks);
                x++;
            }else {
                log.errorv("Stuck waiting for response from sent event w/ pInstanceId: {0}, messageTrigger: {1} to topic: {2}", kogitoProcessInstanceId, messageTrigger, topic);
                proceed=true;
            }
        }
    }

    @After
    public void stop() {
        Optional.ofNullable(kafkaClient).ifPresent(KafkaClient::shutdown);
    }
}

class MissionLifecycleMessageDataEvent extends AbstractProcessDataEvent<com.redhat.cajun.navy.rules.model.Mission> {

    public MissionLifecycleMessageDataEvent() {
    }

    public MissionLifecycleMessageDataEvent(String source, com.redhat.cajun.navy.rules.model.Mission body, String kogitoProcessinstanceId, String kogitoParentProcessinstanceId, String kogitoRootProcessinstanceId, String kogitoProcessId, String kogitoRootProcessId, String kogitoProcessinstanceState, String kogitoReferenceId) {
        this("MissionLifecycleMessageDataEvent", source, body, kogitoProcessinstanceId, kogitoParentProcessinstanceId, kogitoRootProcessinstanceId, kogitoProcessId, kogitoRootProcessId, kogitoProcessinstanceState, kogitoReferenceId);
    }

    public MissionLifecycleMessageDataEvent(String type, String source, com.redhat.cajun.navy.rules.model.Mission body, String kogitoProcessinstanceId, String kogitoParentProcessinstanceId, String kogitoRootProcessinstanceId, String kogitoProcessId, String kogitoRootProcessId, String kogitoProcessinstanceState, String kogitoReferenceId) {
        super(type, source, body, kogitoProcessinstanceId, kogitoParentProcessinstanceId, kogitoRootProcessinstanceId, kogitoProcessId, kogitoRootProcessId, kogitoProcessinstanceState, null, kogitoReferenceId);
    }
}

class MissionWrapper {
    private byte[] missionBytes;

    public byte[] getMissionBytes() {
        return missionBytes;
    }

    public void setMissionBytes(byte[] x) {
        missionBytes = x;
    }
}

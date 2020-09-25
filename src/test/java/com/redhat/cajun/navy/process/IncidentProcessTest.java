package com.redhat.cajun.navy.process;

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

@QuarkusTest
@QuarkusTestResource(KafkaQuarkusTestResource.class)
public class IncidentProcessTest {

    public static final String TOPIC_PRODUCER = "travellers";
    public static final String TOPIC_CONSUMER = "processedtravellers";
    private static Logger LOGGER = LoggerFactory.getLogger(IncidentProcessTest.class);

    @Inject
    private ObjectMapper objectMapper;

    public KafkaClient kafkaClient;

    @ConfigProperty(name = KafkaQuarkusTestResource.KOGITO_KAFKA_PROPERTY)
    private String kafkaBootstrapServers;

    @Test
    public void testProcess() throws InterruptedException {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        kafkaClient = new KafkaClient(kafkaBootstrapServers);

        //number of generated events to test
        final int count = 3;
        final CountDownLatch countDownLatch = new CountDownLatch(count);

        kafkaClient.consume(TOPIC_CONSUMER, s -> {
            LOGGER.info("Received from kafka: {}", s);
            try {
                JsonNode event = objectMapper.readValue(s, JsonNode.class);
                //Traveller traveller = objectMapper.readValue(event.get("data").toString(), Traveller.class);
                countDownLatch.countDown();
            } catch (JsonProcessingException e) {
                LOGGER.error("Error parsing {}", s, e);
                throw new RuntimeException(e);
            }
        });

/*
        IntStream.range(0, count)
                .mapToObj(i -> new Traveller("Name" + i, "LastName" + i, "email" + i, "Nationality" + i))
                 .forEach(traveller -> kafkaClient.produce(generateCloudEvent(traveller), TOPIC_PRODUCER));
*/

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertEquals(countDownLatch.getCount(), 0);
    }

/*
    private String generateCloudEvent(Traveller traveller) {
        assertFalse(traveller.isProcessed());
        try {
            return objectMapper.writeValueAsString(CloudEventBuilder.builder()
                                                           .withId(UUID.randomUUID().toString())
                                                           .withSource(URI.create(""))
                                                           .withType("TravelersMessageDataEvent_3")
                                                           .withTime(ZonedDateTime.now())
                                                           .withData(traveller)
                                                           .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
*/

    @After
    public void stop() {
        Optional.ofNullable(kafkaClient).ifPresent(KafkaClient::shutdown);
    }
}

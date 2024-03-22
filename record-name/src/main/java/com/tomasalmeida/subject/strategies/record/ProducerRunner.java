package com.tomasalmeida.subject.strategies.record;

import com.tomasalmeida.subject.strategies.User;
import com.tomasalmeida.subject.strategies.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static com.tomasalmeida.subject.strategies.common.PropertiesLoader.TOPIC_USERS;

public class ProducerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);

    private final KafkaProducer<String, User> userProducer;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put("clientId", "user-producer");
        userProducer = new KafkaProducer<>(properties);
    }

    public void createEvents() {
        try {
            produceUser("Tomas", "Dias Almeida", "Tomas Almeida", 39);
            produceUser("Fernando", "Perez Machado", "", 53);
            userProducer.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }
    }

    private void produceUser(String firstName, String lastName, String fullName, int age) throws InterruptedException {
        try {
            User user = new User(firstName, lastName, fullName, age);
            LOGGER.info("Sending user {}", user);
            ProducerRecord<String, User> userRecord = new ProducerRecord<>(TOPIC_USERS, user);
            userProducer.send(userRecord, (metadata, exception) -> {
                if (exception == null) {
                    LOGGER.info(">>> sent!");
                } else {
                    LOGGER.error(">>> error!", exception);
                }
            });
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize user: {}", serializationException.getCause().getMessage());
        }
        Thread.sleep(1000);
        LOGGER.info("================");
    }

    public static void main(final String[] args) throws IOException {
        ProducerRunner producerRunner = new ProducerRunner();
        producerRunner.createEvents();
    }
}

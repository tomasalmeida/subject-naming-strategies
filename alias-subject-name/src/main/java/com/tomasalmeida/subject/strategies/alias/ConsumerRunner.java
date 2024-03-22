package com.tomasalmeida.subject.strategies.alias;

import com.tomasalmeida.subject.strategies.User;
import com.tomasalmeida.subject.strategies.common.PropertiesLoader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static com.tomasalmeida.subject.strategies.common.PropertiesLoader.TOPIC_USERS;

public class ConsumerRunner {
    public static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunner.class);

    private final KafkaConsumer<String, User> userConsumer;

    public ConsumerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put("group.id", "user-consumer-group" + System.currentTimeMillis());
        userConsumer = new KafkaConsumer<>(properties);

    }

    private void runConsumer() {
        try {
            userConsumer.subscribe(Collections.singletonList(TOPIC_USERS));
            LOGGER.info("Starting userConsumer...");
            while (true) {
                ConsumerRecords<String, User> records = userConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, User> record : records) {
                    LOGGER.info("User: {}", record.value());
                }
            }
        } catch (WakeupException e) {
            LOGGER.info("Wake up userConsumer...");
        } catch (Exception e) {
            LOGGER.error("Error in useronsumer", e);
        } finally {
            LOGGER.info("Closing userConsumer...");
            userConsumer.close(Duration.ofSeconds(1));
            LOGGER.info("Closed userConsumer...");
        }
    }

    private <T> void closeConsumerAndThread() {
        userConsumer.wakeup();
    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();

        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerRunner.closeConsumerAndThread();

            // join the main thread to give time to consumer to close correctly
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                LOGGER.error("Oh man...", e);
            }
        }));

        consumerRunner.runConsumer();
    }
}

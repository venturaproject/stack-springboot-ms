package com.boilerplate.infrastructure.messaging;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Builds the Kafka producer and consumer explicitly, only when the {@code kafka} profile
 * is active. Spring Boot's Kafka auto-configuration is excluded in {@code application.yml},
 * so with the profile off nothing Kafka-related is on the context and no broker connection
 * is attempted.
 */
@Configuration
@Profile("kafka")
class KafkaConfig {

    @Bean
    ProducerFactory<String, String> eventProducerFactory(
            @Value("${app.events.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.ACKS_CONFIG,
                "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true));
    }

    @Bean
    KafkaTemplate<String, String> eventKafkaTemplate(ProducerFactory<String, String> eventProducerFactory) {
        return new KafkaTemplate<>(eventProducerFactory);
    }

    @Bean
    ConsumerFactory<String, String> eventConsumerFactory(
            @Value("${app.events.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> eventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(eventConsumerFactory);
        return factory;
    }
}

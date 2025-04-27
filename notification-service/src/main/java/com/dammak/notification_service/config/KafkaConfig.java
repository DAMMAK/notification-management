package com.dammak.notification_service.config;

import com.dammak.notification_service.kafka.producer.KafkaProducer;
import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // =================== PRODUCER CONFIGURATION ===================

    // Generic producer factory method
    private <T> ProducerFactory<String, T> createProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // KafkaTemplate beans for different types
    @Bean
    public KafkaTemplate<String, NotificationRequest> notificationRequestKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, NotificationStatus> notificationStatusKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory());
    }

    // Generic producer beans
    @Bean
    public KafkaProducer<NotificationRequest> notificationRequestProducer() {
        return new KafkaProducer<>(notificationRequestKafkaTemplate(), "notification-request-circuit-breaker");
    }

    @Bean
    public KafkaProducer<NotificationStatus> notificationStatusProducer() {
        return new KafkaProducer<>(notificationStatusKafkaTemplate(), "notification-status-circuit-breaker");
    }

//    // For backward compatibility
//    @Bean
//    public KafkaTemplate<String, NotificationStatus> responseKafkaTemplate() {
//        return notificationStatusKafkaTemplate();
//    }

    // =================== CONSUMER CONFIGURATION ===================

    // Generic consumer factory method
    private <T> ConsumerFactory<String, T> createConsumerFactory(Class<T> type, String groupIdSuffix) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + groupIdSuffix);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dammak.notification_service.model");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(type));
    }

    // Consumer factories
    @Bean
    public ConsumerFactory<String, NotificationRequest> notificationConsumerFactory() {
        return createConsumerFactory(NotificationRequest.class, "");
    }

    @Bean
    public ConsumerFactory<String, NotificationStatus> statusConsumerFactory() {
        return createConsumerFactory(NotificationStatus.class, "-response");
    }

    // Listener containers
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> highPriorityKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory());
        factory.setConcurrency(3); // More concurrent consumers for high priority
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> criticalPriorityKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory());
        factory.setConcurrency(5); // Even more concurrent consumers for critical priority
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationStatus> responseListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationStatus> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(statusConsumerFactory());
        return factory;
    }
}
package com.dammak.notification_service.kafka.producer;

import com.dammak.notification_service.model.NotificationRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${notification.kafka.topic.normal}")
    private String normalPriorityTopic;

    @Value("${notification.kafka.topic.high}")
    private String highPriorityTopic;

    @Value("${notification.kafka.topic.critical}")
    private String criticalPriorityTopic;

    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private CircuitBreaker circuitBreaker;
    private RetryConfig standardRetryConfig;
    private RetryConfig criticalRetryConfig;

    private static final String CIRCUIT_BREAKER_NAME = "notificationProducerCircuitBreaker";

    @PostConstruct
    public void init() {
        // Set up Kafka producer
        this.kafkaTemplate = kafkaTemplate();

        // Circuit breaker configuration
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        // Standard retry configuration
        this.standardRetryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(org.apache.kafka.common.errors.RetriableException.class,
                        org.springframework.kafka.KafkaException.class)
                .build();

        // Critical retry configuration - more aggressive
        this.criticalRetryConfig = RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(Exception.class) // Retry on any exception for critical notifications
                .build();

        // Register event handlers for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Circuit breaker state changed: {}", event.getStateTransition()));
    }

    public ProducerFactory<String, NotificationRequest> producerFactory() {
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

    public KafkaTemplate<String, NotificationRequest> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Send normal priority notification
     */
    public CompletableFuture<SendResult<String, NotificationRequest>> sendNormalNotification(NotificationRequest notification) {
        return sendNotification(normalPriorityTopic, notification, standardRetryConfig);
    }

    /**
     * Send high priority notification
     */
    public CompletableFuture<SendResult<String, NotificationRequest>> sendHighPriorityNotification(NotificationRequest notification) {
        return sendNotification(highPriorityTopic, notification, standardRetryConfig);
    }

    /**
     * Send critical priority notification with most aggressive retry pattern
     */
    public CompletableFuture<SendResult<String, NotificationRequest>> sendCriticalNotification(NotificationRequest notification) {
        return sendNotification(criticalPriorityTopic, notification, criticalRetryConfig);
    }

    /**
     * Generic method to send a notification to any topic with specified retry policy
     */
    private CompletableFuture<SendResult<String, NotificationRequest>> sendNotification(
            String topic, NotificationRequest notification, RetryConfig retryConfig) {

        // Use notification ID as the key for consistent partitioning if needed
        String key = notification.getId() != null ? notification.getId().toString() : null;

        log.info("Sending notification to topic {}: {}", topic, notification);

        // Create a retry instance for this specific operation
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        Retry retry = retryRegistry.retry("send-" + topic);

        // Create a supplier that executes the Kafka send operation
        Supplier<CompletableFuture<SendResult<String, NotificationRequest>>> sendOperation = () -> {
            try {
                return kafkaTemplate.send(topic, key, notification)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Notification successfully sent to topic={}, partition={}, offset={}",
                                        topic,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to send notification to topic={}, error={}",
                                        topic, ex.getMessage(), ex);
                                // Re-throw to trigger retry if configured
                                throw new RuntimeException("Failed to send notification", ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Exception caught while sending to Kafka: {}", e.getMessage(), e);
                CompletableFuture<SendResult<String, NotificationRequest>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
            }
        };

        // Apply circuit breaker first
        Supplier<CompletableFuture<SendResult<String, NotificationRequest>>> circuitBreakerProtectedSupplier =
                CircuitBreaker.decorateSupplier(circuitBreaker, sendOperation);

        // Then apply retry
        try {
            return retry.executeSupplier(circuitBreakerProtectedSupplier);
        } catch (Exception e) {
            log.error("Failed to execute send operation with resilience patterns: {}", e.getMessage(), e);
            CompletableFuture<SendResult<String, NotificationRequest>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}
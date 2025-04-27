package com.dammak.notification_service.kafka.producer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
public class KafkaProducer<T> {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaTemplate<String, T> kafkaTemplate;

    private CircuitBreaker circuitBreaker;
    private RetryConfig standardRetryConfig;
    private RetryConfig criticalRetryConfig;

    // Dynamic name to support multiple circuit breakers for different event types
    private final String circuitBreakerName;

    public KafkaProducer(KafkaTemplate<String, T> kafkaTemplate, String circuitBreakerName) {
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerName = circuitBreakerName;
        initResilienceComponents();
    }

    private void initResilienceComponents() {
        // Circuit breaker configuration
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

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

    /**
     * Send event to a topic with standard retry policy
     */
    public CompletableFuture<SendResult<String, T>> sendEvent(String topic, T event) {
        return sendEvent(topic, null, event, standardRetryConfig);
    }

    /**
     * Send event to a topic with standard retry policy and specific key
     */
    public CompletableFuture<SendResult<String, T>> sendEvent(String topic, String key, T event) {
        return sendEvent(topic, key, event, standardRetryConfig);
    }

    /**
     * Send event to a topic with critical retry policy
     */
    public CompletableFuture<SendResult<String, T>> sendCriticalEvent(String topic, T event) {
        return sendEvent(topic, null, event, criticalRetryConfig);
    }

    /**
     * Send event to a topic with critical retry policy and specific key
     */
    public CompletableFuture<SendResult<String, T>> sendCriticalEvent(String topic, String key, T event) {
        return sendEvent(topic, key, event, criticalRetryConfig);
    }

    /**
     * Generic method to send an event with key to any topic with specified retry policy
     */
    private CompletableFuture<SendResult<String, T>> sendEvent(
            String topic, String key, T event, RetryConfig retryConfig) {

        log.info("Sending event to topic {}: {}", topic, event);

        // Create a retry instance for this specific operation
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        Retry retry = retryRegistry.retry("send-" + topic);

        // Create a supplier that executes the Kafka send operation
        Supplier<CompletableFuture<SendResult<String, T>>> sendOperation = () -> {
            try {
                return kafkaTemplate.send(topic, key, event)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Event successfully sent to topic={}, partition={}, offset={}",
                                        topic,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to send event to topic={}, error={}",
                                        topic, ex.getMessage(), ex);
                                // Re-throw to trigger retry if configured
                                throw new RuntimeException("Failed to send event", ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Exception caught while sending to Kafka: {}", e.getMessage(), e);
                CompletableFuture<SendResult<String, T>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
            }
        };

        // Apply circuit breaker first
        Supplier<CompletableFuture<SendResult<String, T>>> circuitBreakerProtectedSupplier =
                CircuitBreaker.decorateSupplier(circuitBreaker, sendOperation);

        // Then apply retry
        try {
            return retry.executeSupplier(circuitBreakerProtectedSupplier);
        } catch (Exception e) {
            log.error("Failed to execute send operation with resilience patterns: {}", e.getMessage(), e);
            CompletableFuture<SendResult<String, T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }


}
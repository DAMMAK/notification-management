package com.dammak.notification_service.kafka.listener;



import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationListener {

    private final NotificationService notificationService;


    @KafkaListener(
            topics = "${notification.kafka.topic.normal}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenNormalPriority(NotificationRequest request) {
        log.info("Received normal priority notification request: {}", request);
        try {
            NotificationStatus status = notificationService.processNotification(request);
            log.info("Normal priority notification processed: {}", status);
        } catch (Exception e) {
            log.error("Error processing normal priority notification: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topic.high}",
            groupId = "${spring.kafka.consumer.group-id}-high",
            containerFactory = "highPriorityKafkaListenerContainerFactory")
    public void listenHighPriority(NotificationRequest request, Acknowledgment acknowledgment) {
        log.info("Received high priority notification request: {}", request);
        try {
            NotificationStatus status = notificationService.processNotification(request);
            log.info("High priority notification processed: {}", status);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing high priority notification: {}", e.getMessage(), e);
            // Don't acknowledge to allow reprocessing
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topic.critical}",
            groupId = "${spring.kafka.consumer.group-id}-critical",
            containerFactory = "criticalPriorityKafkaListenerContainerFactory")
    public void listenCriticalPriority(NotificationRequest request, Acknowledgment acknowledgment) {
        log.info("Received CRITICAL priority notification request: {}", request);
        try {
            NotificationStatus status = notificationService.processNotification(request);
            log.info("Critical priority notification processed: {}", status);
            acknowledgment.acknowledge();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing critical priority notification: {}", e.getMessage(), e);
            // Retry immediately once before returning to Kafka
            try {
                log.info("Immediate retry for critical notification");
                NotificationStatus status = notificationService.processNotification(request);
                log.info("Critical priority notification processed on retry: {}", status);
                acknowledgment.acknowledge();
            } catch (Exception retryEx) {
                log.error("Error on immediate retry for critical notification: {}", retryEx.getMessage());
                // Don't acknowledge to allow reprocessing by Kafka
            }
        }
    }
}
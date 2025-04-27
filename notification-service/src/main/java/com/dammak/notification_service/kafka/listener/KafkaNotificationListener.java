package com.dammak.notification_service.kafka.listener;



import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationListener {

    private final NotificationService notificationService;
    private final KafkaTemplate<String, NotificationStatus> kafkaTemplate;

    @Value("${notification.kafka.topic.status-events}")
    private String responseTopic;


    private void sendResponse(NotificationRequest request, NotificationStatus status, Exception exception) {


        kafkaTemplate.send(responseTopic, request.getId().toString(), status)
                .whenComplete((record, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent response for notification {}", request.getId());
                    }else{
                        log.error("Failed to send response for notification {}", request.getId(), ex);
                    }
                });
    }


    @KafkaListener(
            topics = "${notification.kafka.topic.normal}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenNormalPriority(NotificationRequest request) {
        log.info("Received normal priority notification request: {}", request);
        try {
            processNotification(request);
        } catch (Exception e) {
            log.error("Error processing normal priority notification: {}", e.getMessage(), e);
        }
    }

    private void processNotification(NotificationRequest request) {
        log.info("Received notification request: {}", request);
        try {
            NotificationStatus status = notificationService.processNotification(request);
            log.info("Notification processed: {}", status);
            sendResponse(request, status, null);
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
            sendResponse(request, NotificationStatus.getError(request,"Error processing notification"), e);
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
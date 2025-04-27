package com.dammak.notification_service.service;

import com.dammak.notification_service.kafka.producer.KafkaProducer;
import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final NotificationHistoryService historyService;
    private final KafkaProducer<NotificationRequest> notificationRequestKafkaProducer;
    private final KafkaProducer<NotificationStatus> notificationStatusKafkaProducer;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${notification.kafka.topic.normal}")
    private String normalPriorityTopic;

    @Value("${notification.kafka.topic.high}")
    private String highPriorityTopic;

    @Value("${notification.kafka.topic.critical}")
    private String criticalPriorityTopic;
    @Value("${notification.kafka.topic.status-events}")
    private String responseTopic;

    public NotificationStatus processNotification(NotificationRequest request) {
        log.info("Processing notification request: {}", request);

        NotificationStatus status = switch (request.getType()) {
            case EMAIL -> emailService.sendEmail(request);
            case SMS -> smsService.sendSms(request);
            case PUSH -> pushNotificationService.sendPushNotification(request);
            default -> throw new IllegalArgumentException("Unsupported notification type: " + request.getType());
        };

        // Save notification history
        historyService.saveNotificationHistory(status);

        notificationStatusKafkaProducer.sendEvent(responseTopic, status.getId().toString(), status); // Send success to notification-status-event topic

        return status;
    }

    public void sendNotification(NotificationRequest request) throws ExecutionException, InterruptedException {
        log.info("Sending notification request: {}", request);
        CompletableFuture<SendResult<String, NotificationRequest>> response = switch (request.getPriority()) {
            case HIGH -> notificationRequestKafkaProducer.sendEvent(highPriorityTopic, request.getId().toString(), request);
            case NORMAL -> notificationRequestKafkaProducer.sendEvent(normalPriorityTopic, request.getId().toString(), request);
            case CRITICAL -> notificationRequestKafkaProducer.sendEvent(criticalPriorityTopic, request.getId().toString(), request);
            default -> throw new IllegalArgumentException("Unsupported priority: " + request.getPriority());
        };
       response.thenApply(result -> {
            log.info("Notification sent successfully: {}", result);
           return null;
       }).exceptionally(ex -> {
            log.error("Failed to send notification", ex);
            NotificationStatus errorStatus = NotificationStatus.getError(request, ex.getMessage());
            notificationStatusKafkaProducer.sendEvent(responseTopic, errorStatus.getId().toString(), errorStatus); // Send error to notification-status-event topic
            return null;
        });


    }

}
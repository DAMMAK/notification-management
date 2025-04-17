package com.dammak.notification_service.service;

import com.dammak.notification_service.kafka.producer.KafkaNotificationProducer;
import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    //    private final PushNotificationService pushNotificationService;
    private final NotificationHistoryService historyService;
    private final KafkaNotificationProducer kafkaNotificationProducer;


    public NotificationStatus processNotification(NotificationRequest request) {
        log.info("Processing notification request: {}", request);

        NotificationStatus status = switch (request.getType()) {
            case EMAIL -> emailService.sendEmail(request);
            case SMS -> smsService.sendSms(request);
//            case PUSH -> pushNotificationService.sendPushNotification(request);
            default -> throw new IllegalArgumentException("Unsupported notification type: " + request.getType());
        };

        // Save notification history
        historyService.saveNotificationHistory(status);

        return status;
    }

    public NotificationStatus sendNotification(NotificationRequest request) throws ExecutionException, InterruptedException {
        log.info("Sending notification request: {}", request);
        CompletableFuture<SendResult<String, NotificationRequest>> response = switch (request.getPriority()) {
            case HIGH -> kafkaNotificationProducer.sendHighPriorityNotification(request);
            case NORMAL -> kafkaNotificationProducer.sendNormalNotification(request);
            case CRITICAL -> kafkaNotificationProducer.sendCriticalNotification(request);
            default -> throw new IllegalArgumentException("Unsupported priority: " + request.getPriority());
        };
        var data = response.thenApply(result -> {
            log.info("Notification sent successfully: {}", result);
            return NotificationStatus.getSuccess(request);
        }).exceptionally(ex -> {
            log.error("Failed to send notification", ex);
             return NotificationStatus.getError(request);
        });
        return data.get();
    }
}
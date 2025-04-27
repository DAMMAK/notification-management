package com.dammak.notification_service.service;

import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PushNotificationService {


    @CircuitBreaker(name = "pushService")
    @Retry(name = "pushService")
    public NotificationStatus sendPushNotification(NotificationRequest request) {
        try {
             final FirebaseMessaging firebaseMessaging= FirebaseMessaging.getInstance();
            Notification notification = Notification.builder()
                    .setTitle(request.getSubject())
                    .setBody(request.getContent())
                    .build();

            Message message = Message.builder()
                    .setToken(request.getRecipient())
                    .setNotification(notification)
                    .putAllData(request.getTemplateData())
                    .build();

            String response = firebaseMessaging.send(message);

            log.info("Push notification sent successfully to {}, response: {}", request.getRecipient(), response);
            return NotificationStatus.getSuccess(request, "Push notification sent successfully");

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification to {}: {}", request.getRecipient(), e.getMessage());
            return NotificationStatus.getError(request, "Failed to send push notification: " + e.getMessage());

        }
    }
}
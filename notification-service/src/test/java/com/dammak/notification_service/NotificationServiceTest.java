package com.dammak.notification_service;


import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private NotificationHistoryService historyService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequest emailRequest;
    private NotificationRequest smsRequest;
    private NotificationRequest pushRequest;

    @BeforeEach
    void setUp() {
        // Email request
        emailRequest = new NotificationRequest();
        emailRequest.setType(NotificationRequest.NotificationType.EMAIL);
        emailRequest.setRecipient("test@example.com");
        emailRequest.setSubject("Test Email");
        emailRequest.setContent("This is a test email content");

        // SMS request
        smsRequest = new NotificationRequest();
        smsRequest.setType(NotificationRequest.NotificationType.SMS);
        smsRequest.setRecipient("+1234567890");
        smsRequest.setContent("This is a test SMS content");

        // Push notification request
        pushRequest = new NotificationRequest();
        pushRequest.setType(NotificationRequest.NotificationType.PUSH);
        pushRequest.setRecipient("device_token_123");
        pushRequest.setSubject("Test Push Notification");
        pushRequest.setContent("This is a test push notification content");
        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        pushRequest.setTemplateData(data);
    }

    @Test
    void testProcessEmailNotification() {
        // Arrange
        NotificationStatus expectedStatus = new NotificationStatus(
                UUID.randomUUID(),
                NotificationRequest.NotificationType.EMAIL,
                emailRequest.getRecipient(),
                true,
                "Email sent successfully",
                LocalDateTime.now()
        );

        when(emailService.sendEmail(any(NotificationRequest.class))).thenReturn(expectedStatus);
        doNothing().when(historyService).saveNotificationHistory(any(NotificationStatus.class));

        // Act
        NotificationStatus result = notificationService.processNotification(emailRequest);

        // Assert
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(emailService, times(1)).sendEmail(emailRequest);
        verify(historyService, times(1)).saveNotificationHistory(expectedStatus);
        verify(smsService, never()).sendSms(any());
        verify(pushNotificationService, never()).sendPushNotification(any());
    }

    @Test
    void testProcessSmsNotification() {
        // Arrange
        NotificationStatus expectedStatus = new NotificationStatus(
                UUID.randomUUID(),
                NotificationRequest.NotificationType.SMS,
                smsRequest.getRecipient(),
                true,
                "SMS sent successfully",
                LocalDateTime.now()
        );

        when(smsService.sendSms(any(NotificationRequest.class))).thenReturn(expectedStatus);
        doNothing().when(historyService).saveNotificationHistory(any(NotificationStatus.class));

        // Act
        NotificationStatus result = notificationService.processNotification(smsRequest);

        // Assert
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(smsService, times(1)).sendSms(smsRequest);
        verify(historyService, times(1)).saveNotificationHistory(expectedStatus);
        verify(emailService, never()).sendEmail(any());
        verify(pushNotificationService, never()).sendPushNotification(any());
    }

    @Test
    void testProcessPushNotification() {
        // Arrange
        NotificationStatus expectedStatus = new NotificationStatus(
                UUID.randomUUID(),
                NotificationRequest.NotificationType.PUSH,
                pushRequest.getRecipient(),
                true,
                "Push notification sent successfully",
                LocalDateTime.now()
        );

        when(pushNotificationService.sendPushNotification(any(NotificationRequest.class))).thenReturn(expectedStatus);
        doNothing().when(historyService).saveNotificationHistory(any(NotificationStatus.class));

        // Act
        NotificationStatus result = notificationService.processNotification(pushRequest);

        // Assert
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(pushNotificationService, times(1)).sendPushNotification(pushRequest);
        verify(historyService, times(1)).saveNotificationHistory(expectedStatus);
        verify(emailService, never()).sendEmail(any());
        verify(smsService, never()).sendSms(any());
    }
}
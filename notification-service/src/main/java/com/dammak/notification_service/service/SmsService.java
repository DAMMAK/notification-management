package com.dammak.notification_service.service;



import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.Counter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    private final Counter smsSuccessCounter;
    private final Counter smsFailureCounter;
    private final FailedNotificationService failedNotificationService;

    private boolean initialized = false;

    private void initTwilio() {
        if (!initialized) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            initialized = true;
        }
    }

    @CircuitBreaker(name = "smsService", fallbackMethod = "fallbackSms")
    @Retry(name = "smsService")
    public NotificationStatus sendSms(NotificationRequest request) {
        try {
            initTwilio();

            String content = request.getContent();
            if (content == null || content.isEmpty()) {
                content = "No content provided";
            }

            Message message = Message.creator(
                    new PhoneNumber(request.getRecipient()),
                    new PhoneNumber(twilioPhoneNumber),
                    content
            ).create();

            smsSuccessCounter.increment();
            log.info("SMS sent successfully to {}, SID: {}", request.getRecipient(), message.getSid());
            return NotificationStatus.getSuccess(request,"SMS sent successfully");

        } catch (Exception e) {
            smsFailureCounter.increment();
            log.error("Failed to send SMS to {}: {}", request.getRecipient(), e.getMessage());
            failedNotificationService.saveFailedNotification(request, e.getMessage());
            return NotificationStatus.getError(request, "Failed to send SMS: " + e.getMessage());
        }
    }

    public NotificationStatus fallbackSms(NotificationRequest request, Exception e) {
        log.error("Circuit breaker triggered for SMS to {}: {}", request.getRecipient(), e.getMessage());
        failedNotificationService.saveFailedNotification(request, "Circuit breaker triggered: " + e.getMessage());
        return NotificationStatus.getError(request, "SMS service temporarily unavailable");
    }
}
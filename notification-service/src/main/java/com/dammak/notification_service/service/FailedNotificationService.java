package com.dammak.notification_service.service;


import com.dammak.notification_service.entity.FailedNotification;
import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.repository.FailedNotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedNotificationService {

    private final FailedNotificationRepository repository;
    private final ObjectMapper objectMapper;

    public void saveFailedNotification(NotificationRequest request, String errorMessage) {
        try {
            FailedNotification failedNotification = new FailedNotification();
            failedNotification.setId(UUID.randomUUID().toString());
            failedNotification.setType(request.getType());
            failedNotification.setRecipient(request.getRecipient());
            failedNotification.setSubject(request.getSubject());
            failedNotification.setContent(request.getContent());
            failedNotification.setTemplateName(request.getTemplateName());

            if (request.getTemplateData() != null) {
                try {
                    failedNotification.setTemplateData(objectMapper.writeValueAsString(request.getTemplateData()));
                } catch (JsonProcessingException e) {
                    log.error("Error serializing template data: {}", e.getMessage());
                }
            }

            failedNotification.setRetryCount(0);
            failedNotification.setCreatedAt(LocalDateTime.now());
            failedNotification.setErrorMessage(errorMessage);

            repository.save(failedNotification);
            log.info("Saved failed notification: {}", failedNotification.getId());
        } catch (Exception e) {
            log.error("Error saving failed notification: {}", e.getMessage());
        }
    }

    public NotificationRequest mapToNotificationRequest(FailedNotification failedNotification) {
        NotificationRequest request = new NotificationRequest();
        request.setType(failedNotification.getType());
        request.setRecipient(failedNotification.getRecipient());
        request.setSubject(failedNotification.getSubject());
        request.setContent(failedNotification.getContent());
        request.setTemplateName(failedNotification.getTemplateName());

        if (failedNotification.getTemplateData() != null && !failedNotification.getTemplateData().isEmpty()) {
            try {
                request.setTemplateData(objectMapper.readValue(
                        failedNotification.getTemplateData(),
                        objectMapper.getTypeFactory().constructMapType(
                                java.util.Map.class, String.class, Object.class)
                ));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing template data: {}", e.getMessage());
            }
        }

        return request;
    }

    public void updateRetryAttempt(String id) {
        repository.findById(id).ifPresent(notification -> {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setLastRetry(LocalDateTime.now());
            repository.save(notification);
        });
    }
}
package com.dammak.notification_service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private UUID id = UUID.randomUUID(); // auto-generated UUID

    private Priority priority = Priority.NORMAL; // default to NORMAL

    @NotNull
    private NotificationType type;

    @NotBlank
    private String recipient; // email, phone number, or device token

    @NotBlank
    private String subject;

    private String content;

    private String templateName;

    private Map<String, String> templateData;

    public enum NotificationType {
        EMAIL, SMS, PUSH
    }
    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }
}
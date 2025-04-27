package com.dammak.notification_service.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatus {
    private UUID id;
    private NotificationRequest.NotificationType type;
    private String recipient;
    private boolean success;
    private String message;
    private LocalDateTime sentAt;

    // Success factory method
    public static NotificationStatus getSuccess(NotificationRequest request, String message) {
        return new NotificationStatus(
                request.getId(),
                request.getType(),
                request.getRecipient(),
                true,
                message == null ? request.getType() + " sent successfully" : message,
                LocalDateTime.now()
        );
    }

    // Error factory method
    public static NotificationStatus getError(NotificationRequest request, String message) {
        return new NotificationStatus(
                request.getId(),
                request.getType(),
                request.getRecipient(),
                false,
                message == null ? request.getType() + " failed" : message,
                LocalDateTime.now()
        );
    }
}
package com.dammak.notification_service.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatus {
    private String id;
    private NotificationRequest.NotificationType type;
    private String recipient;
    private boolean success;
    private String message;
    private LocalDateTime sentAt;

    // Success factory method
    public static NotificationStatus getSuccess(NotificationRequest request) {
        return new NotificationStatus(
                request.getId().toString(),
                request.getType(),
                request.getRecipient(),
                true,
                request.getType() + " sent successfully",
                LocalDateTime.now()
        );
    }

    // Error factory method
    public static NotificationStatus getError(NotificationRequest request) {
        return new NotificationStatus(
                request.getId().toString(),
                request.getType(),
                request.getRecipient(),
                false,
                request.getType() + " failed",
                LocalDateTime.now()
        );
    }
}
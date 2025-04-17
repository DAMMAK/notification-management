package com.dammak.notification_service.entity;


import com.dammak.notification_service.model.NotificationRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedNotification {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private NotificationRequest.NotificationType type;

    private String recipient;

    private String subject;

    @Column(length = 1000)
    private String content;

    private String templateName;

    @Column(length = 1000)
    private String templateData; // JSON string

    private int retryCount = 0;

    private LocalDateTime createdAt;

    private LocalDateTime lastRetry;

    @Column(length = 500)
    private String errorMessage;
}
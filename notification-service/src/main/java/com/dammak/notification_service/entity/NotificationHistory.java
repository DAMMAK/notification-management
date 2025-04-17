package com.dammak.notification_service.entity;


import com.dammak.notification_service.model.NotificationRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private NotificationRequest.NotificationType type;

    private String recipient;

    private String subject;

    @Column(length = 1000)
    private String content;

    private boolean success;

    @Column(length = 500)
    private String errorMessage;

    private LocalDateTime sentAt;
}
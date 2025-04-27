package com.dammak.notification_service.service;


import com.dammak.notification_service.entity.NotificationHistory;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryService {

    private final NotificationHistoryRepository repository;

    public void saveNotificationHistory(NotificationStatus status) {
        try {
            NotificationHistory history = new NotificationHistory();
            history.setId(status.getId().toString());
            history.setType(status.getType());
            history.setRecipient(status.getRecipient());
            history.setSuccess(status.isSuccess());
            history.setErrorMessage(status.isSuccess() ? null : status.getMessage());
            history.setSentAt(status.getSentAt());

            repository.save(history);
            log.debug("Saved notification history: {}", history.getId());
        } catch (Exception e) {
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }
}
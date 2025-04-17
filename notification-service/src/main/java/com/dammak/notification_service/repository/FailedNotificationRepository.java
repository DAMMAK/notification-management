package com.dammak.notification_service.repository;


import com.dammak.notification_service.entity.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedNotificationRepository extends JpaRepository<FailedNotification, String> {

    @Query("SELECT f FROM FailedNotification f WHERE f.retryCount < :maxRetries ORDER BY f.createdAt ASC")
    List<FailedNotification> findNotificationsToRetry(int maxRetries, int limit);
    int deleteByRetryCountGreaterThanEqual(int retryCount);
}
//package com.dammak.notification_service.scheduler;
//
//
//
//import com.dammak.notification_service.entity.FailedNotification;
//import com.dammak.notification_service.model.NotificationRequest;
//import com.dammak.notification_service.model.NotificationStatus;
//import com.dammak.notification_service.repository.FailedNotificationRepository;
//import com.dammak.notification_service.service.FailedNotificationService;
//import com.dammak.notification_service.service.NotificationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationRetryScheduler {
//
//
//    private final FailedNotificationRepository failedNotificationRepository;
//    private final FailedNotificationService failedNotificationService;
//    private final NotificationService notificationService;
//
//    @Value("${notification.retry.max-attempts}")
//    private int maxRetryAttempts;
//
//    @Value("${notification.retry.batch-size:50}")
//    private int batchSize;
////
////    @Scheduled(fixedDelayString = "${notification.retry.interval}")
////    @SchedulerLock(name = "retryFailedNotifications", lockAtMostFor = "PT5M")
//    public void retryFailedNotifications() {
//        log.info("Starting scheduled retry for failed notifications");
//
//        List<FailedNotification> failedNotifications =
//                failedNotificationRepository.findNotificationsToRetry(maxRetryAttempts, batchSize);
//
//        log.info("Found {} failed notifications to retry", failedNotifications.size());
//
//        for (FailedNotification failedNotification : failedNotifications) {
//            try {
//                NotificationRequest request = failedNotificationService.mapToNotificationRequest(failedNotification);
//                NotificationStatus status = notificationService.processNotification(request);
//
//                if (status.isSuccess()) {
//                    // If successful, delete from failed notifications
//                    failedNotificationRepository.deleteById(failedNotification.getId());
//                    log.info("Successfully retried notification {}", failedNotification.getId());
//                } else {
//                    // Update retry count
//                    failedNotificationService.updateRetryAttempt(failedNotification.getId());
//                    log.warn("Retry attempt failed for notification {}: {}",
//                            failedNotification.getId(), status.getMessage());
//                }
//            } catch (Exception e) {
//                failedNotificationService.updateRetryAttempt(failedNotification.getId());
//                log.error("Error retrying notification {}: {}",
//                        failedNotification.getId(), e.getMessage());
//            }
//        }
//
//        // Clean up notifications that exceeded max retry attempts
//        int deleted = failedNotificationRepository.deleteByRetryCountGreaterThanEqual(maxRetryAttempts);
//
//        if (deleted > 0) {
//            log.info("Deleted {} notifications that exceeded maximum retry attempts", deleted);
//        }
//
//        log.info("Completed scheduled retry for failed notifications");
//    }
//}
package com.dammak.notification_service.controller;


import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.service.NotificationService;
import io.netty.handler.timeout.TimeoutException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final Map<UUID, CompletableFuture<NotificationStatus>> pendingResponses = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, NotificationStatus> kafkaTemplate;



    @PostMapping
    public ResponseEntity<?> sendNotification(@Valid @RequestBody NotificationRequest request) throws ExecutionException, InterruptedException, TimeoutException {

        log.info("Received direct notification request: {}", request);

        // Create a future to hold the response
        CompletableFuture<NotificationStatus> responseFuture = new CompletableFuture<>();
        pendingResponses.put(request.getId(), responseFuture);

        // Send the notification
        notificationService.sendNotification(request);

        try {
            // Wait for the response with timeout
            NotificationStatus response = responseFuture.get(180, TimeUnit.SECONDS);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            pendingResponses.remove(request.getId());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Notification processing timeout");
        }
    }
    @GetMapping("/")
        public String getHome() {
        return "Welcome to Notification Service";
        }

    @KafkaListener(
            topics = "${notification.kafka.topic.status-events}",
            groupId = "${spring.kafka.consumer.group-id}-status",
            containerFactory = "responseListenerContainerFactory")
    public void handleNotificationResponse(NotificationStatus response) {
        CompletableFuture<NotificationStatus> future = pendingResponses.remove(response.getId());
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("Received response for unknown notification ID: {}", response.getId());
        }
    }

}
package com.dammak.notification_service.controller;



import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import com.dammak.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;


    @PostMapping
    public ResponseEntity<?> sendNotification(@Valid @RequestBody NotificationRequest request) throws ExecutionException, InterruptedException {
        log.info("Received direct notification request: {}", request);
      var data = notificationService.sendNotification(request);
        return ResponseEntity.ok(data);
    }
    @GetMapping("/")
        public String getHome() {
        return "Welcome to Notification Service";
        }
}
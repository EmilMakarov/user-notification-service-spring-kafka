package org.example.usernotificationservicespringkafka.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.usernotificationservicespringkafka.dto.NotificationRequest;
import org.example.usernotificationservicespringkafka.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@Valid @RequestBody NotificationRequest request) {
        notificationService.sendManualNotification(request.getEmail(), request.getMessage());
        return ResponseEntity.ok("Notification sent successfully");
    }
}
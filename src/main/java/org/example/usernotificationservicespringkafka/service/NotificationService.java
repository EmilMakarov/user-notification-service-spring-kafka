package org.example.usernotificationservicespringkafka.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.usernotificationservicespringkafka.event.UserEvent;
import org.example.usernotificationservicespringkafka.event.UserEventType;
import org.example.usernotificationservicespringkafka.model.Notification;
import org.example.usernotificationservicespringkafka.repository.NotificationRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final JavaMailSender sendMail;
    private final NotificationRepository notificationRepository;

    public void handleEvent(UserEvent userEvent) {
        String message;
        if (UserEventType.CREATED == userEvent.getEventType()) {
            message = "Profile created successfully";
        } else if (UserEventType.DELETED == userEvent.getEventType()) {
            message = "Profile is deleted";
        } else {
            message = "Unsupported event type";
        }
        sendEmail(userEvent.getEmail(), "notification", message, userEvent.getEventType().name());
    }

    public void sendManualNotification(String email, String message) {
        sendEmail(email, "notification", message, "manual");
    }

    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallbackLogger")
    private void sendEmail(String to, String subject, String text, String type) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(text);
        sendMail.send(mailMessage);
        saveNotification(to, text, type);
    }
    private void emailFallbackLogger(String to, String subject, String text, String type, Throwable t) {
        log.warn("Email sending failed for {}: {}", to, t.getMessage());
    }


    private void saveNotification(String email, String message, String type) {
        Notification notification = Notification.builder()
                .email(email)
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }
}
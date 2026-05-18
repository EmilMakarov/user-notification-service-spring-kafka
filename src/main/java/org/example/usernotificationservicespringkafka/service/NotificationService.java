package org.example.usernotificationservicespringkafka.service;

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
        if (userEvent.getType() == UserEventType.CREATED) {
            message = "Notification has been created";
        } else if (userEvent.getType() == UserEventType.DELETED) {
            message = "Notification has been deleted";
        } else {
            message = "Unsupported event type";
        }
        sendEmail(userEvent.getEmail(), "уведомление", message);
        saveNotification(userEvent.getEmail(), message, userEvent.getType().name());
    }

    public void sendManualNotification(String email, String message) {
        sendEmail(email, "уведомление", message);
        saveNotification(email, message, "manual");
    }

    private void sendEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        sendMail.send(mailMessage);
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
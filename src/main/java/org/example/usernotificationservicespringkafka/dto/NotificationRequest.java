package org.example.usernotificationservicespringkafka.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String email;
    private String message;
}
package org.example.usernotificationservicespringkafka.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class NotificationRequest {
    @NotBlank(message = "Поле не может быть пустым")
    @Email(message = "Некорректный email")
    private String email;
    @NotBlank(message = "Поле не может быть пустым")
    private String message;
}
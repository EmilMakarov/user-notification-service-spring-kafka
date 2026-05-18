package org.example.usernotificationservicespringkafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("notifications")
public class Notification {
    private String id;
    private String email;
    private String message;
    private String type;
    private LocalDateTime timestamp;
}
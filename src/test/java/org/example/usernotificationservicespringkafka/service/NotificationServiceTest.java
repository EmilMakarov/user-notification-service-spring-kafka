package org.example.usernotificationservicespringkafka.service;

import org.example.usernotificationservicespringkafka.config.TestProducerConfig;
import org.example.usernotificationservicespringkafka.event.UserEvent;
import org.example.usernotificationservicespringkafka.event.UserEventType;
import org.example.usernotificationservicespringkafka.model.Notification;
import org.example.usernotificationservicespringkafka.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestProducerConfig.class)
class NotificationServiceTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));
    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> mongoContainer = new GenericContainer<>(DockerImageName.parse("mongo:8.2.9")).withExposedPorts(27017);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String testId = UUID.randomUUID().toString().substring(0, 8);
        registry.add("spring.mongodb.uri", () -> "mongodb://" + mongoContainer.getHost() + ":" + mongoContainer.getFirstMappedPort() + "/notificationdb");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group-" + testId);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private JavaMailSender mailSender;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${app.kafka.topic.user-events}")
    private String topic;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reset(mailSender);
    }

    @Test
    void SendEmailAndSaveNotificationOnCreateTest() {
        UserEvent event = new UserEvent(UserEventType.CREATED, "aboba@yandex.ru");
        notificationService.handleEvent(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification doc = notifications.get(0);
        assertAll(() -> assertEquals("aboba@yandex.ru", doc.getEmail()), () -> assertEquals("CREATED", doc.getType()), () -> assertTrue(doc.getMessage().contains("создан")));
    }

    @Test
    void SendEmailAndSaveNotificationOnDeleteTest() {
        UserEvent event = new UserEvent(UserEventType.DELETED, "aboba@yandex.ru");
        notificationService.handleEvent(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification doc = notifications.get(0);
        assertAll(() -> assertEquals("aboba@yandex.ru", doc.getEmail()), () -> assertEquals("DELETED", doc.getType()), () -> assertTrue(doc.getMessage().contains("удалён")));
    }

    @Test
    void CreateEvenKafkaTest() {
        UserEvent event = new UserEvent(UserEventType.CREATED, "aboba@yandex.ru");
        kafkaTemplate.send(topic, event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(mailSender).send(any(SimpleMailMessage.class));
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            Notification doc = notifications.get(0);
            assertEquals("aboba@yandex.ru", doc.getEmail());
            assertEquals("CREATED", doc.getType());
            assertTrue(doc.getMessage().contains("создан"));
        });
    }

    @Test
    void DeleteEventKafkaTest() {
        UserEvent event = new UserEvent(UserEventType.DELETED, "aboba@yandex.ru");
        kafkaTemplate.send(topic, event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(mailSender).send(any(SimpleMailMessage.class));
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            Notification doc = notifications.get(0);
            assertEquals("aboba@yandex.ru", doc.getEmail());
            assertEquals("DELETED", doc.getType());
            assertTrue(doc.getMessage().contains("удалён"));
        });
    }

    @Test
    void CustomNotificationTest() throws Exception {
        String requestJson = """
                {
                "email":"aboba@yandex.ru",
                "message":"hello"
                }""";

        mockMvc.perform(post("/api/notifications/send").contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isOk()).andExpect(content().string("Notification sent successfully"));

        verify(mailSender, timeout(3000)).send(any(SimpleMailMessage.class));
        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification doc = notifications.get(0);
        assertAll(() -> assertEquals("aboba@yandex.ru", doc.getEmail()), () -> assertEquals("hello", doc.getMessage()), () -> assertEquals("manual", doc.getType()));
    }

    @Test
    void BadRequestTest() throws Exception {
        String requestJson = """
                {
                "email":"asdasda",
                "message":"123"
                }""";

        mockMvc.perform(post("/api/notifications/send").contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isBadRequest());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertTrue(notificationRepository.findAll().isEmpty());
    }

    @Test
    void BadRequestEmptyEmailTest() throws Exception {
        String requestJson = """
                {
                "email":"",
                "message":"123"
                }""";

        mockMvc.perform(post("/api/notifications/send").contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isBadRequest());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertTrue(notificationRepository.findAll().isEmpty());
    }

    @Test
    void BadRequestEmptyMessageTest() throws Exception {
        String requestJson = """
                {
                "email":"aboba@yandex.ru",
                "message":""
                }""";

        mockMvc.perform(post("/api/notifications/send").contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isBadRequest());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertTrue(notificationRepository.findAll().isEmpty());
    }
}
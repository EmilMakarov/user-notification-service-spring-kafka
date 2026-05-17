package org.example.usernotificationservicespringkafka;

import org.springframework.boot.SpringApplication;

public class TestUserNotificationServiceSpringKafkaApplication {

	public static void main(String[] args) {
		SpringApplication.from(UserNotificationServiceSpringKafkaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

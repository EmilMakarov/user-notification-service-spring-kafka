package org.example.usernotificationservicespringkafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.usernotificationservicespringkafka.event.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, UserEvent> userEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TYPE_MAPPINGS,
                "org.example.userservicespring.event.UserEvent:org.example.usernotificationservicespringkafka.event.UserEvent");
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "org.example.userservicespring.event,org.example.usernotificationservicespringkafka.event");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userEventConsumerFactory());
        return factory;
    }
}

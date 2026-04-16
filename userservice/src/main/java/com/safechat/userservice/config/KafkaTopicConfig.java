package com.safechat.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder.name("user.deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeleteStatusTopic() {
        return TopicBuilder.name("user.delete.status")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
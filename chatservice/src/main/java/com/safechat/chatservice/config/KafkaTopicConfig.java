package com.safechat.chatservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageSend() {
        return TopicBuilder.name("chat.message.normal.send").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messagePrivacySend() {
        return TopicBuilder.name("chat.message.privacy.send").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messageEdit() {
        return TopicBuilder.name("chat.message.edit").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messageDelete() {
        return TopicBuilder.name("chat.message.delete").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messageDelivery() {
        return TopicBuilder.name("chat.message.delivery").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic conversationCreate() {
        return TopicBuilder.name("chat.conversation.create").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic conversationDelete() {
        return TopicBuilder.name("chat.conversation.delete").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic typing() {
        return TopicBuilder.name("chat.typing").partitions(3).replicas(1).build();
    }
}
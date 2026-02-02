package com.diddycart.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Profile("!test")
public class KafkaConfig {

    // Define the user-registration topic
    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name("user-registration").partitions(2).replicas(1).build();
    }

    // Define the order-placed topic
    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order-placed").partitions(2).replicas(1).build();
    }

    // Define a ThreadPoolTaskExecutor for Kafka event processing
    @Bean(name = "kafkaWorkerPool")
    public Executor kafkaWorkerPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("KafkaWorker-");
        executor.initialize();
        return executor;
    }
}
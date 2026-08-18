package dev.pubfleet.controlplane.messaging;

import dev.pubfleet.contracts.JobCompletedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates the one topic on start, so a cold clone needs no manual broker step.
 *
 * <p>One partition and one replica. Partition count is a unit 01 question, because it
 * caps worker parallelism only when the broker dispatches work, and here it does not.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic jobCompletedTopic() {
        return TopicBuilder.name(JobCompletedEvent.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

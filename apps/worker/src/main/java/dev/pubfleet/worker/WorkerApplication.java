package dev.pubfleet.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * The worker. It owns the claim and nothing else.
 *
 * <p>No web server runs here. The worker polls Postgres for work and reports the
 * outcome on Kafka. It never writes a terminal status: that belongs to the control
 * plane.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Bean
    public JobRunner jobRunner(WorkerProperties properties) {
        return new JobRunner(properties.workDelay());
    }

    /** Injected rather than called statically, so time is testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

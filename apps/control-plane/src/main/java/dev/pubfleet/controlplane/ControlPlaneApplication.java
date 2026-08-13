package dev.pubfleet.controlplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** The control plane. It owns the job record and the terminal state. */
@SpringBootApplication
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }

    /** Injected rather than called statically, so time is testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

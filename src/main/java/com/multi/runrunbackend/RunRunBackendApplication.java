package com.multi.runrunbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RunRunBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunRunBackendApplication.class, args);
    }

}

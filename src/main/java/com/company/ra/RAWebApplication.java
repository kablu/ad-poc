package com.company.ra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot Application for RA Web Service
 */
@SpringBootApplication
@EnableJpaRepositories
public class RAWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RAWebApplication.class, args);
    }
}

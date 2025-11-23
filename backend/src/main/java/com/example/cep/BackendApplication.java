package com.example.cep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Main Spring Boot Application for Chaos Engineering Platform Control Plane
 *
 * This is the entry point for the backend service that manages chaos experiments.
 *
 * Currently using in-memory storage for rapid development and testing.
 * Database auto-configuration is disabled until JPA entities are implemented.
 *
 * @author Zară Mihnea-Tudor
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}

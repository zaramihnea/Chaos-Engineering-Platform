package com.example.cep.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 *
 * Configures the API documentation UI and metadata for the Chaos Engineering Platform.
 *
 * Access the Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access the OpenAPI JSON at: http://localhost:8080/v3/api-docs
 *
 * @author Zară Mihnea-Tudor
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chaosEngineeringOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Chaos Engineering Platform - Control Plane API")
                .description("""
                    REST API for managing chaos engineering experiments in Kubernetes clusters.
                    """)
                .version("1.0.0")
                )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
                new Server()
                    .url("http://staging.chaos.example.com")
                    .description("Staging Environment"),
                new Server()
                    .url("https://chaos.example.com")
                    .description("Production Environment")
            ));
    }
}

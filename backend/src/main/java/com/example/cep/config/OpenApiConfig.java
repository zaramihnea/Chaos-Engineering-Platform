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

                    ## Features
                    - Create and manage chaos experiments
                    - Schedule experiment runs (immediate or scheduled)
                    - Monitor experiment execution and results
                    - Policy validation and approval workflows
                    - SLO-based automatic abort logic
                    - Integration with Prometheus for metrics

                    ## Experiment Workflow
                    1. **Create Experiment** - Define fault type, target, and SLOs
                    2. **Validate Policy** - Ensure experiment meets organizational policies
                    3. **Approve** (if needed) - High-risk experiments require approval
                    4. **Schedule Run** - Execute immediately or schedule for later
                    5. **Monitor** - Track run state and SLO metrics
                    6. **Review Report** - Analyze results and SLO compliance

                    ## Supported Fault Types
                    - POD_KILL - Terminate pods to test resilience
                    - CPU_HOG - Stress CPU to test performance degradation
                    - MEMORY_HOG - Stress memory to test OOM scenarios
                    - NETWORK_DELAY - Add latency to test timeout handling
                    - NETWORK_PARTITION - Simulate network splits

                    ## SLO Metrics
                    - LATENCY_P95 / LATENCY_P99 - Response time percentiles
                    - ERROR_RATE - Percentage of failed requests
                    - AVAILABILITY - Service uptime percentage
                    - THROUGHPUT - Requests per second
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Zară Mihnea-Tudor")
                    .email("mihnea.zara@example.com")
                    .url("https://github.com/zaramihnea/Chaos-Engineering-Platform"))
                .license(new License()
                    .name("Academic Project")
                    .url("https://github.com/zaramihnea/Chaos-Engineering-Platform/blob/main/LICENSE")))
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

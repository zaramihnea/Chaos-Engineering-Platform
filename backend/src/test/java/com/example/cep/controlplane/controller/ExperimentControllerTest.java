package com.example.cep.controlplane.controller;

import com.example.cep.controlplane.api.ControlPlaneApi;
import com.example.cep.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test-Driven Development (TDD) tests for ExperimentController
 *
 * Following TDD principles:
 * 1. Write failing test (RED)
 * 2. Implement minimal code to pass (GREEN)
 * 3. Refactor while keeping tests green (REFACTOR)
 *
 * Test Coverage:
 * - Happy path scenarios
 * - Error handling (4xx, 5xx)
 * - Edge cases
 * - Input validation
 * - HTTP status codes
 * - Response formats
 *
 * @author Zară Mihnea-Tudor
 */
@WebMvcTest(ExperimentController.class)
@DisplayName("Experiment Controller Integration Tests")
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ControlPlaneApi controlPlaneApi;

    private ExperimentDefinition validExperiment;
    private static final String EXPERIMENT_ID = "test-experiment-123";
    private static final String RUN_ID = "test-run-456";

    @BeforeEach
    void setUp() {
        // Setup valid test data
        TargetSystem target = new TargetSystem("dev-cluster", "default", Map.of());
        SloTarget slo = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_requests[5m]))",
            500.0,
            "<"
        );

        validExperiment = new ExperimentDefinition(
            null,
            "Test Experiment",
            FaultType.POD_KILL,
            Map.of("podSelector", "app=test"),
            target,
            Duration.ofMinutes(5),
            List.of(slo),
            true,
            "test-user"
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE EXPERIMENT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/experiments - Create Experiment")
    class CreateExperimentTests {

        @Test
        @DisplayName("Should create experiment and return 201 with experiment ID")
        void createExperiment_ValidDefinition_Returns201() throws Exception {
            // Given
            when(controlPlaneApi.createExperiment(any(ExperimentDefinition.class)))
                .thenReturn(EXPERIMENT_ID);

            // When & Then
            mockMvc.perform(post("/api/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validExperiment)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.experimentId").value(EXPERIMENT_ID))
                .andExpect(jsonPath("$.message").value("Experiment created successfully"));

            verify(controlPlaneApi, times(1)).createExperiment(any(ExperimentDefinition.class));
        }

        @Test
        @DisplayName("Should return 400 when experiment violates policy")
        void createExperiment_PolicyViolation_Returns400() throws Exception {
            // Given
            when(controlPlaneApi.createExperiment(any(ExperimentDefinition.class)))
                .thenThrow(new RuntimeException("Policy violation: Invalid cluster"));

            // When & Then
            mockMvc.perform(post("/api/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validExperiment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Policy violation")));

            verify(controlPlaneApi, times(1)).createExperiment(any(ExperimentDefinition.class));
        }

        @Test
        @DisplayName("Should return 400 when request body is invalid JSON")
        void createExperiment_InvalidJson_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                .andExpect(status().isBadRequest());

            verify(controlPlaneApi, never()).createExperiment(any());
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void createExperiment_MissingFields_Returns400() throws Exception {
            // Given - experiment with null name
            when(controlPlaneApi.createExperiment(any(ExperimentDefinition.class)))
                .thenThrow(new RuntimeException("Validation error: Name is required"));

            ExperimentDefinition invalidExperiment = new ExperimentDefinition(
                null, null, FaultType.POD_KILL, Map.of(), null, null, List.of(), true, null
            );

            // When & Then
            mockMvc.perform(post("/api/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidExperiment)))
                .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // LIST EXPERIMENTS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/experiments - List Experiments")
    class ListExperimentsTests {

        @Test
        @DisplayName("Should return empty list when no experiments exist")
        void listExperiments_NoExperiments_ReturnsEmptyList() throws Exception {
            // Given
            when(controlPlaneApi.listExperiments()).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/experiments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

            verify(controlPlaneApi, times(1)).listExperiments();
        }

        @Test
        @DisplayName("Should return list of experiments when they exist")
        void listExperiments_ExperimentsExist_ReturnsList() throws Exception {
            // Given
            ExperimentDefinition exp1 = new ExperimentDefinition(
                "exp-1", "Exp 1", FaultType.POD_KILL, Map.of(),
                new TargetSystem("dev", "default", Map.of()),
                Duration.ofMinutes(1), List.of(), true, "user1"
            );
            ExperimentDefinition exp2 = new ExperimentDefinition(
                "exp-2", "Exp 2", FaultType.CPU_STRESS, Map.of(),
                new TargetSystem("staging", "default", Map.of()),
                Duration.ofMinutes(2), List.of(), true, "user2"
            );

            when(controlPlaneApi.listExperiments()).thenReturn(List.of(exp1, exp2));

            // When & Then
            mockMvc.perform(get("/api/experiments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("exp-1"))
                .andExpect(jsonPath("$[0].name").value("Exp 1"))
                .andExpect(jsonPath("$[1].id").value("exp-2"))
                .andExpect(jsonPath("$[1].name").value("Exp 2"));

            verify(controlPlaneApi, times(1)).listExperiments();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SCHEDULE RUN TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/experiments/{id}/runs - Schedule Run")
    class ScheduleRunTests {

        @Test
        @DisplayName("Should schedule dry run and return 201")
        void scheduleRun_DryRun_Returns201() throws Exception {
            // Given
            when(controlPlaneApi.scheduleRun(eq(EXPERIMENT_ID), any(Instant.class), eq(true)))
                .thenReturn(RUN_ID);

            String requestBody = """
                {
                    "when": null,
                    "dryRun": true
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/runs", EXPERIMENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(RUN_ID))
                .andExpect(jsonPath("$.message").value("Run scheduled successfully"))
                .andExpect(jsonPath("$.dryRun").value("true"));

            verify(controlPlaneApi, times(1))
                .scheduleRun(eq(EXPERIMENT_ID), any(Instant.class), eq(true));
        }

        @Test
        @DisplayName("Should schedule production run when dryRun is false")
        void scheduleRun_ProductionRun_Returns201() throws Exception {
            // Given
            when(controlPlaneApi.scheduleRun(eq(EXPERIMENT_ID), any(Instant.class), eq(false)))
                .thenReturn(RUN_ID);

            String requestBody = """
                {
                    "when": null,
                    "dryRun": false
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/runs", EXPERIMENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dryRun").value("false"));
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void scheduleRun_ExperimentNotFound_Returns404() throws Exception {
            // Given
            when(controlPlaneApi.scheduleRun(any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Experiment not found"));

            String requestBody = """
                {
                    "when": null,
                    "dryRun": true
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/runs", "invalid-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should default to dryRun=false when not specified")
        void scheduleRun_NoDryRunFlag_DefaultsToFalse() throws Exception {
            // Given
            when(controlPlaneApi.scheduleRun(eq(EXPERIMENT_ID), any(Instant.class), eq(false)))
                .thenReturn(RUN_ID);

            String requestBody = """
                {
                    "when": null
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/runs", EXPERIMENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dryRun").value("false"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET RUN STATE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/runs/{runId}/state - Get Run State")
    class GetRunStateTests {

        @Test
        @DisplayName("Should return run state when run exists")
        void getRunState_RunExists_ReturnsState() throws Exception {
            // Given
            when(controlPlaneApi.getRunState(RUN_ID)).thenReturn(RunState.RUNNING);

            // When & Then
            mockMvc.perform(get("/api/runs/{runId}/state", RUN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(RUN_ID))
                .andExpect(jsonPath("$.state").value("RUNNING"));

            verify(controlPlaneApi, times(1)).getRunState(RUN_ID);
        }

        @Test
        @DisplayName("Should return 404 when run not found")
        void getRunState_RunNotFound_Returns404() throws Exception {
            // Given
            when(controlPlaneApi.getRunState("invalid-run"))
                .thenThrow(new RuntimeException("Run not found"));

            // When & Then
            mockMvc.perform(get("/api/runs/{runId}/state", "invalid-run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should handle all run states correctly")
        void getRunState_AllStates_ReturnCorrectly() throws Exception {
            for (RunState state : RunState.values()) {
                when(controlPlaneApi.getRunState(RUN_ID)).thenReturn(state);

                mockMvc.perform(get("/api/runs/{runId}/state", RUN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value(state.toString()));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET REPORT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/runs/{runId}/report - Get Report")
    class GetReportTests {

        @Test
        @DisplayName("Should return report when run completed")
        void getReport_RunCompleted_ReturnsReport() throws Exception {
            // Given
            Report report = new Report(
                RUN_ID,
                "Test Experiment",
                Instant.now().minusSeconds(300),
                Instant.now(),
                RunState.COMPLETED,
                Map.of("latency_p95", 450.0)
            );
            when(controlPlaneApi.getReport(RUN_ID)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/runs/{runId}/report", RUN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(RUN_ID))
                .andExpect(jsonPath("$.outcome").value("COMPLETED"))
                .andExpect(jsonPath("$.experimentName").value("Test Experiment"));

            verify(controlPlaneApi, times(1)).getReport(RUN_ID);
        }

        @Test
        @DisplayName("Should return 404 when report not found")
        void getReport_NotFound_Returns404() throws Exception {
            // Given
            when(controlPlaneApi.getReport("invalid-run"))
                .thenThrow(new RuntimeException("Report not found"));

            // When & Then
            mockMvc.perform(get("/api/runs/{runId}/report", "invalid-run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ABORT RUN TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/runs/{runId} - Abort Run")
    class AbortRunTests {

        @Test
        @DisplayName("Should abort run with custom reason")
        void abortRun_CustomReason_Returns200() throws Exception {
            // Given
            doNothing().when(controlPlaneApi).abortRun(RUN_ID, "SLO breach detected");

            String requestBody = """
                {
                    "reason": "SLO breach detected"
                }
                """;

            // When & Then
            mockMvc.perform(delete("/api/runs/{runId}", RUN_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(RUN_ID))
                .andExpect(jsonPath("$.message").value("Run aborted successfully"))
                .andExpect(jsonPath("$.reason").value("SLO breach detected"));

            verify(controlPlaneApi, times(1)).abortRun(RUN_ID, "SLO breach detected");
        }

        @Test
        @DisplayName("Should use default reason when not provided")
        void abortRun_NoReason_UsesDefault() throws Exception {
            // Given
            doNothing().when(controlPlaneApi).abortRun(RUN_ID, "Manual abort");

            String requestBody = "{}";

            // When & Then
            mockMvc.perform(delete("/api/runs/{runId}", RUN_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Manual abort"));
        }

        @Test
        @DisplayName("Should return 404 when run not found")
        void abortRun_RunNotFound_Returns404() throws Exception {
            // Given
            doThrow(new RuntimeException("Run not found"))
                .when(controlPlaneApi).abortRun(anyString(), anyString());

            String requestBody = """
                {
                    "reason": "Test"
                }
                """;

            // When & Then
            mockMvc.perform(delete("/api/runs/{runId}", "invalid-run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // APPROVE EXPERIMENT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/experiments/{id}/approve - Approve Experiment")
    class ApproveExperimentTests {

        @Test
        @DisplayName("Should approve experiment and return approval ID")
        void approveExperiment_ValidRequest_Returns200() throws Exception {
            // Given
            String approvalId = "approval-789";
            when(controlPlaneApi.approveExperiment(EXPERIMENT_ID, "admin@example.com"))
                .thenReturn(approvalId);

            String requestBody = """
                {
                    "approver": "admin@example.com"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/approve", EXPERIMENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentId").value(EXPERIMENT_ID))
                .andExpect(jsonPath("$.approvalId").value(approvalId))
                .andExpect(jsonPath("$.approver").value("admin@example.com"))
                .andExpect(jsonPath("$.message").value("Experiment approved successfully"));

            verify(controlPlaneApi, times(1))
                .approveExperiment(EXPERIMENT_ID, "admin@example.com");
        }

        @Test
        @DisplayName("Should use default approver when not specified")
        void approveExperiment_NoApprover_UsesDefault() throws Exception {
            // Given
            when(controlPlaneApi.approveExperiment(EXPERIMENT_ID, "system"))
                .thenReturn("approval-123");

            String requestBody = "{}";

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/approve", EXPERIMENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approver").value("system"));
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void approveExperiment_ExperimentNotFound_Returns404() throws Exception {
            // Given
            when(controlPlaneApi.approveExperiment(anyString(), anyString()))
                .thenThrow(new RuntimeException("Experiment not found"));

            String requestBody = """
                {
                    "approver": "admin@example.com"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/experiments/{id}/approve", "invalid-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATE POLICY TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/experiments/validate - Validate Policy")
    class ValidatePolicyTests {

        @Test
        @DisplayName("Should return valid when experiment passes policy")
        void validatePolicy_ValidExperiment_ReturnsTrue() throws Exception {
            // Given
            when(controlPlaneApi.validatePolicy(any(ExperimentDefinition.class)))
                .thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/experiments/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validExperiment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Experiment passes all policy checks"));

            verify(controlPlaneApi, times(1)).validatePolicy(any(ExperimentDefinition.class));
        }

        @Test
        @DisplayName("Should return invalid when experiment violates policy")
        void validatePolicy_InvalidExperiment_ReturnsFalse() throws Exception {
            // Given
            when(controlPlaneApi.validatePolicy(any(ExperimentDefinition.class)))
                .thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/experiments/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validExperiment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Experiment violates policy"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HEALTH CHECK TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/health - Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return UP status")
        void health_Always_ReturnsUp() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Chaos Engineering Control Plane"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EDGE CASES AND ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle missing Content-Type header gracefully")
        void missingContentType_Returns415() throws Exception {
            mockMvc.perform(post("/api/experiments")
                    .content(objectMapper.writeValueAsString(validExperiment)))
                .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle empty request body")
        void emptyRequestBody_Returns400() throws Exception {
            mockMvc.perform(post("/api/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle null path variable")
        void nullPathVariable_Returns404() throws Exception {
            mockMvc.perform(get("/api/runs/null/state"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle special characters in path variable")
        void specialCharactersInPath_HandledCorrectly() throws Exception {
            String runIdWithSpecialChars = "run-123-abc!@#";
            when(controlPlaneApi.getRunState(runIdWithSpecialChars))
                .thenReturn(RunState.RUNNING);

            mockMvc.perform(get("/api/runs/{runId}/state", runIdWithSpecialChars))
                .andExpect(status().isOk());
        }
    }
}

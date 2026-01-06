package com.example.cep.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RunPlan Model Tests")
class RunPlanTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void testConstructor() {
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-001", "Test Experiment", FaultType.POD_KILL, Map.of(),
            new TargetSystem("cluster", "default", Map.of()),
            Duration.ofMinutes(5), List.of(), true, "user"
        );

        Instant scheduledTime = Instant.now();
        RunPlan runPlan = new RunPlan("run-001", definition, scheduledTime, false);

        assertEquals("run-001", runPlan.getRunId());
        assertEquals(definition, runPlan.getDefinition());
        assertEquals(scheduledTime, runPlan.getScheduledAt());
        assertFalse(runPlan.isDryRun());
    }

    @Test
    @DisplayName("isDryRun returns correct value")
    void testDryRun() {
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-002", "Test", FaultType.CPU_STRESS, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
            List.of(), false, "user"
        );

        RunPlan dryRunEnabled = new RunPlan("run-002", definition, Instant.now(), true);
        RunPlan dryRunDisabled = new RunPlan("run-003", definition, Instant.now(), false);

        assertTrue(dryRunEnabled.isDryRun());
        assertFalse(dryRunDisabled.isDryRun());
    }

    @Test
    @DisplayName("getDefinition returns correct experiment")
    void testGetDefinition() {
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-003", "Network Delay Test", FaultType.NETWORK_DELAY, Map.of("delay", "100ms"),
            new TargetSystem("prod", "default", Map.of("app", "api")),
            Duration.ofMinutes(10), List.of(), false, "admin"
        );

        RunPlan runPlan = new RunPlan("run-004", definition, Instant.now(), false);

        assertEquals("exp-003", runPlan.getDefinition().getId());
        assertEquals("Network Delay Test", runPlan.getDefinition().getName());
        assertEquals(FaultType.NETWORK_DELAY, runPlan.getDefinition().getFaultType());
    }

    @Test
    @DisplayName("handles different scheduled times")
    void testScheduledTimes() {
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-004", "Test", FaultType.MEMORY_STRESS, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
            List.of(), false, "user"
        );

        Instant now = Instant.now();
        Instant future = now.plusSeconds(3600);
        Instant past = now.minusSeconds(3600);

        RunPlan immediateRun = new RunPlan("run-005", definition, now, false);
        RunPlan scheduledRun = new RunPlan("run-006", definition, future, false);
        RunPlan pastRun = new RunPlan("run-007", definition, past, false);

        assertEquals(now, immediateRun.getScheduledAt());
        assertEquals(future, scheduledRun.getScheduledAt());
        assertEquals(past, pastRun.getScheduledAt());
    }

    @Test
    @DisplayName("handles different run IDs")
    void testRunIds() {
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-005", "Test", FaultType.POD_KILL, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
            List.of(), false, "user"
        );

        RunPlan run1 = new RunPlan("run-12345", definition, Instant.now(), false);
        RunPlan run2 = new RunPlan("run-67890", definition, Instant.now(), false);
        RunPlan run3 = new RunPlan("custom-run-id", definition, Instant.now(), false);

        assertEquals("run-12345", run1.getRunId());
        assertEquals("run-67890", run2.getRunId());
        assertEquals("custom-run-id", run3.getRunId());
    }

    @Test
    @DisplayName("preserves all experiment definition properties")
    void testPreservesDefinitionProperties() {
        SloTarget slo = new SloTarget(SloMetric.LATENCY_P95, "query", 500.0, "<");
        Map<String, Object> params = Map.of("intensity", 80, "duration", "30s");

        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-006", "Complex Experiment", FaultType.CPU_STRESS, params,
            new TargetSystem("prod-cluster", "production", Map.of("env", "prod")),
            Duration.ofMinutes(15), List.of(slo), true, "admin-user"
        );

        RunPlan runPlan = new RunPlan("run-008", definition, Instant.now(), false);

        assertEquals("exp-006", runPlan.getDefinition().getId());
        assertEquals("Complex Experiment", runPlan.getDefinition().getName());
        assertEquals(FaultType.CPU_STRESS, runPlan.getDefinition().getFaultType());
        assertEquals(2, runPlan.getDefinition().getParameters().size());
        assertEquals("prod-cluster", runPlan.getDefinition().getTarget().getCluster());
        assertEquals(1, runPlan.getDefinition().getSlos().size());
        assertTrue(runPlan.getDefinition().isDryRunAllowed());
        assertEquals("admin-user", runPlan.getDefinition().getCreatedBy());
    }
}

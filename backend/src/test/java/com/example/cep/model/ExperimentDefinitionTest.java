package com.example.cep.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExperimentDefinition Model Tests")
class ExperimentDefinitionTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void testConstructor() {
        Map<String, Object> params = new HashMap<>();
        params.put("intensity", 80);

        TargetSystem target = new TargetSystem("prod-cluster", "default", Map.of("app", "cart"));
        SloTarget slo = new SloTarget(SloMetric.LATENCY_P95, "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))", 500.0, "<");

        ExperimentDefinition experiment = new ExperimentDefinition(
            "exp-001",
            "CPU Stress Test",
            FaultType.CPU_STRESS,
            params,
            target,
            Duration.ofMinutes(5),
            List.of(slo),
            true,
            "test-user"
        );

        assertEquals("exp-001", experiment.getId());
        assertEquals("CPU Stress Test", experiment.getName());
        assertEquals(FaultType.CPU_STRESS, experiment.getFaultType());
        assertEquals(params, experiment.getParameters());
        assertEquals(target, experiment.getTarget());
        assertEquals(Duration.ofMinutes(5), experiment.getTimeout());
        assertEquals(1, experiment.getSlos().size());
        assertTrue(experiment.isDryRunAllowed());
        assertEquals("test-user", experiment.getCreatedBy());
    }

    @Test
    @DisplayName("getParameters returns correct map")
    void testGetParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("duration", "30s");
        params.put("workers", 4);

        ExperimentDefinition experiment = new ExperimentDefinition(
            "exp-002", "Test", FaultType.POD_KILL, params,
            new TargetSystem("cluster", "ns", Map.of()), Duration.ofMinutes(1),
            List.of(), false, "user"
        );

        assertEquals("30s", experiment.getParameters().get("duration"));
        assertEquals(4, experiment.getParameters().get("workers"));
    }

    @Test
    @DisplayName("getSlos returns correct list")
    void testGetSlos() {
        SloTarget slo1 = new SloTarget(SloMetric.LATENCY_P95, "query1", 100.0, "<");
        SloTarget slo2 = new SloTarget(SloMetric.ERROR_RATE, "query2", 0.01, "<");

        ExperimentDefinition experiment = new ExperimentDefinition(
            "exp-003", "Test", FaultType.NETWORK_DELAY, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofSeconds(30),
            List.of(slo1, slo2), true, "user"
        );

        assertEquals(2, experiment.getSlos().size());
        assertEquals(SloMetric.LATENCY_P95, experiment.getSlos().get(0).getMetric());
        assertEquals(SloMetric.ERROR_RATE, experiment.getSlos().get(1).getMetric());
    }

    @Test
    @DisplayName("handles empty SLOs list")
    void testEmptySlos() {
        ExperimentDefinition experiment = new ExperimentDefinition(
            "exp-004", "No SLOs", FaultType.MEMORY_STRESS, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(2),
            List.of(), false, "user"
        );

        assertNotNull(experiment.getSlos());
        assertTrue(experiment.getSlos().isEmpty());
    }

    @Test
    @DisplayName("handles all fault types")
    void testAllFaultTypes() {
        for (FaultType faultType : FaultType.values()) {
            ExperimentDefinition experiment = new ExperimentDefinition(
                "exp-" + faultType.name(), "Test", faultType, Map.of(),
                new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
                List.of(), false, "user"
            );

            assertEquals(faultType, experiment.getFaultType());
        }
    }

    @Test
    @DisplayName("isDryRunAllowed returns correct value")
    void testDryRunAllowed() {
        ExperimentDefinition dryRunEnabled = new ExperimentDefinition(
            "exp-005", "Dry Run", FaultType.POD_KILL, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
            List.of(), true, "user"
        );

        ExperimentDefinition dryRunDisabled = new ExperimentDefinition(
            "exp-006", "No Dry Run", FaultType.POD_KILL, Map.of(),
            new TargetSystem("c", "n", Map.of()), Duration.ofMinutes(1),
            List.of(), false, "user"
        );

        assertTrue(dryRunEnabled.isDryRunAllowed());
        assertFalse(dryRunDisabled.isDryRunAllowed());
    }
}

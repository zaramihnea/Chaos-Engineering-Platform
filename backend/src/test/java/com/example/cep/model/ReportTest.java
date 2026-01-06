package com.example.cep.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Report Model Tests")
class ReportTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void testConstructor() {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(300);
        Map<String, Object> sloDeltas = Map.of("latency_p95", 450.0, "error_rate", 0.005);

        Report report = new Report(
            "run-001",
            "CPU Stress Test",
            startTime,
            endTime,
            RunState.COMPLETED,
            sloDeltas
        );

        assertEquals("run-001", report.getRunId());
        assertEquals("CPU Stress Test", report.getExperimentName());
        assertEquals(startTime, report.getStartedAt());
        assertEquals(endTime, report.getEndedAt());
        assertEquals(RunState.COMPLETED, report.getOutcome());
        assertEquals(sloDeltas, report.getSloDeltas());
    }

    @Test
    @DisplayName("handles all RunState outcomes")
    void testAllOutcomes() {
        Instant now = Instant.now();

        for (RunState state : RunState.values()) {
            Report report = new Report(
                "run-" + state.name(),
                "Test",
                now,
                now.plusSeconds(60),
                state,
                Map.of()
            );

            assertEquals(state, report.getOutcome());
        }
    }

    @Test
    @DisplayName("getSloDeltas returns correct map")
    void testGetSloDeltas() {
        Map<String, Object> sloDeltas = new HashMap<>();
        sloDeltas.put("latency_p95", 350.0);
        sloDeltas.put("latency_p99", 500.0);
        sloDeltas.put("error_rate", 0.001);
        sloDeltas.put("availability", 99.99);

        Report report = new Report(
            "run-002",
            "Test",
            Instant.now(),
            Instant.now().plusSeconds(120),
            RunState.COMPLETED,
            sloDeltas
        );

        assertEquals(4, report.getSloDeltas().size());
        assertEquals(350.0, report.getSloDeltas().get("latency_p95"));
        assertEquals(500.0, report.getSloDeltas().get("latency_p99"));
        assertEquals(0.001, report.getSloDeltas().get("error_rate"));
        assertEquals(99.99, report.getSloDeltas().get("availability"));
    }

    @Test
    @DisplayName("handles empty SLO deltas")
    void testEmptySloDeltas() {
        Report report = new Report(
            "run-003",
            "No SLOs",
            Instant.now(),
            Instant.now().plusSeconds(60),
            RunState.COMPLETED,
            Map.of()
        );

        assertNotNull(report.getSloDeltas());
        assertTrue(report.getSloDeltas().isEmpty());
    }

    @Test
    @DisplayName("handles failed outcome")
    void testFailedOutcome() {
        Report report = new Report(
            "run-004",
            "Failed Experiment",
            Instant.now(),
            Instant.now().plusSeconds(30),
            RunState.FAILED,
            Map.of("latency_p95", 1200.0)
        );

        assertEquals(RunState.FAILED, report.getOutcome());
        assertEquals("Failed Experiment", report.getExperimentName());
    }

    @Test
    @DisplayName("handles aborted outcome")
    void testAbortedOutcome() {
        Report report = new Report(
            "run-005",
            "Aborted Experiment",
            Instant.now(),
            Instant.now().plusSeconds(15),
            RunState.ABORTED,
            Map.of()
        );

        assertEquals(RunState.ABORTED, report.getOutcome());
    }

    @Test
    @DisplayName("handles blocked by policy outcome")
    void testBlockedByPolicyOutcome() {
        Report report = new Report(
            "run-006",
            "Blocked Experiment",
            Instant.now(),
            Instant.now(),
            RunState.BLOCKED_BY_POLICY,
            Map.of()
        );

        assertEquals(RunState.BLOCKED_BY_POLICY, report.getOutcome());
    }

    @Test
    @DisplayName("calculates duration from timestamps")
    void testDuration() {
        Instant startTime = Instant.parse("2024-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2024-01-01T10:05:00Z");

        Report report = new Report(
            "run-007",
            "Timed Experiment",
            startTime,
            endTime,
            RunState.COMPLETED,
            Map.of()
        );

        Duration duration = Duration.between(report.getStartedAt(), report.getEndedAt());
        assertEquals(300, duration.getSeconds());
    }

    @Test
    @DisplayName("handles complex SLO delta values")
    void testComplexSloDeltas() {
        Map<String, Object> sloDeltas = new HashMap<>();
        sloDeltas.put("latency_p95", 450.0);
        sloDeltas.put("latency_p99", 650.0);
        sloDeltas.put("error_rate", 0.002);
        sloDeltas.put("availability", 99.95);
        sloDeltas.put("throughput", 1500);
        sloDeltas.put("custom_metric", "pass");

        Report report = new Report(
            "run-008",
            "Complex Report",
            Instant.now(),
            Instant.now().plusSeconds(180),
            RunState.COMPLETED,
            sloDeltas
        );

        assertEquals(6, report.getSloDeltas().size());
        assertTrue(report.getSloDeltas().containsKey("custom_metric"));
        assertEquals("pass", report.getSloDeltas().get("custom_metric"));
    }
}

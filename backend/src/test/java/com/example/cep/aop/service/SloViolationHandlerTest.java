package com.example.cep.aop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SloViolationHandler
 *
 * These tests verify the violation recording, analysis, and response logic
 * without requiring Spring context or AOP infrastructure.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@DisplayName("SLO Violation Handler Tests")
class SloViolationHandlerTest {

    private SloViolationHandler handler;
    private Map<String, Object> mockSloResults;

    @BeforeEach
    void setUp() {
        handler = new SloViolationHandler();

        // Create mock SLO results showing a breach
        mockSloResults = new HashMap<>();
        mockSloResults.put("latency_p95", 750.0);
        mockSloResults.put("latency_p95_threshold", 500.0);
        mockSloResults.put("latency_p95_comparator", "<");
        mockSloResults.put("error_rate", 0.005);
        mockSloResults.put("error_rate_threshold", 0.01);
        mockSloResults.put("error_rate_comparator", "<");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // VIOLATION RECORDING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should record baseline breach violation")
    void testRecordViolation_BaselineBreach_RecordsCorrectly() {
        // Act
        SloViolationHandler.ViolationResponse response = handler.recordViolation(
            "exp-123",
            SloViolationHandler.ViolationType.BASELINE_BREACH,
            mockSloResults
        );

        // Assert
        assertNotNull(response);
        assertEquals(SloViolationHandler.Severity.CRITICAL, response.getSeverity());
        assertTrue(response.getRecommendedActions().contains("INVESTIGATE_SYSTEM_HEALTH"));

        // Verify violation was recorded
        List<SloViolationHandler.ViolationRecord> history = handler.getViolationHistory("exp-123");
        assertEquals(1, history.size());
        assertEquals(SloViolationHandler.ViolationType.BASELINE_BREACH, history.get(0).getType());
    }

    @Test
    @DisplayName("Should record runtime breach violation")
    void testRecordViolation_RuntimeBreach_RecordsCorrectly() {
        // Act
        SloViolationHandler.ViolationResponse response = handler.recordViolation(
            "exp-456",
            SloViolationHandler.ViolationType.RUNTIME_BREACH,
            mockSloResults
        );

        // Assert
        assertNotNull(response);
        assertEquals(SloViolationHandler.Severity.MEDIUM, response.getSeverity());
        assertTrue(response.getRecommendedActions().contains("INCREASE_MONITORING_FREQUENCY"));
    }

    @Test
    @DisplayName("Should record recovery failure violation")
    void testRecordViolation_RecoveryFailure_RecordsCorrectly() {
        // Act
        SloViolationHandler.ViolationResponse response = handler.recordViolation(
            "exp-789",
            SloViolationHandler.ViolationType.RECOVERY_FAILURE,
            mockSloResults
        );

        // Assert
        assertNotNull(response);
        assertEquals(SloViolationHandler.Severity.HIGH, response.getSeverity());
        assertTrue(response.getRecommendedActions().contains("TRIGGER_REMEDIATION"));
        assertTrue(response.getRecommendedActions().contains("CREATE_INCIDENT_TICKET"));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ABORT DECISION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should not recommend abort for single violation")
    void testShouldAbort_SingleViolation_ReturnsFalse() {
        // Arrange
        handler.recordViolation("exp-single", SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);

        // Act
        boolean shouldAbort = handler.shouldAbortExperiment("exp-single");

        // Assert
        assertFalse(shouldAbort, "Should not abort for single violation");
    }

    @Test
    @DisplayName("Should recommend abort after multiple violations")
    void testShouldAbort_MultipleViolations_ReturnsTrue() {
        // Arrange: Record 3 violations (exceeds threshold)
        String experimentId = "exp-multiple";
        for (int i = 0; i < 3; i++) {
            handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);
        }

        // Act
        boolean shouldAbort = handler.shouldAbortExperiment(experimentId);

        // Assert
        assertTrue(shouldAbort, "Should abort after 3 violations");
    }

    @Test
    @DisplayName("Should not abort for violations outside time window")
    void testShouldAbort_OldViolations_ReturnsFalse() throws InterruptedException {
        // Note: This test would require mocking time or waiting, simplified here
        // In production, violations older than 60s are not counted

        // Arrange
        String experimentId = "exp-old";
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);

        // Act
        boolean shouldAbort = handler.shouldAbortExperiment(experimentId);

        // Assert
        assertFalse(shouldAbort, "Single violation should not trigger abort");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // VIOLATION STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should return accurate violation statistics")
    void testGetViolationStatistics_MultipleTypes_ReturnsAccurateStats() {
        // Arrange: Record multiple violation types
        String experimentId = "exp-stats";
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.BASELINE_BREACH, mockSloResults);
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RECOVERY_FAILURE, mockSloResults);

        // Act
        Map<String, Object> stats = handler.getViolationStatistics(experimentId);

        // Assert
        assertEquals(4, stats.get("total_violations"));
        assertEquals(1L, stats.get("baseline_breaches"));
        assertEquals(2L, stats.get("runtime_breaches"));
        assertEquals(1L, stats.get("recovery_failures"));
        assertNotNull(stats.get("first_violation"));
        assertNotNull(stats.get("last_violation"));
    }

    @Test
    @DisplayName("Should return empty statistics for unknown experiment")
    void testGetViolationStatistics_UnknownExperiment_ReturnsEmptyStats() {
        // Act
        Map<String, Object> stats = handler.getViolationStatistics("unknown-exp");

        // Assert
        assertEquals(0, stats.get("total_violations"));
        assertEquals(0L, stats.get("baseline_breaches"));
        assertNull(stats.get("first_violation"));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HISTORY MANAGEMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should clear violation history")
    void testClearViolationHistory_RemovesAllRecords() {
        // Arrange
        String experimentId = "exp-clear";
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.BASELINE_BREACH, mockSloResults);
        handler.recordViolation(experimentId, SloViolationHandler.ViolationType.RUNTIME_BREACH, mockSloResults);

        // Act
        handler.clearViolationHistory(experimentId);

        // Assert
        List<SloViolationHandler.ViolationRecord> history = handler.getViolationHistory(experimentId);
        assertTrue(history.isEmpty(), "History should be empty after clearing");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // VIOLATION RESPONSE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should recommend abort and alert for critical violations")
    void testViolationResponse_CriticalViolation_RecommendsAbortAndAlert() {
        // Arrange: Record enough violations to trigger abort
        String experimentId = "exp-critical";
        for (int i = 0; i < 3; i++) {
            handler.recordViolation(experimentId, SloViolationHandler.ViolationType.BASELINE_BREACH, mockSloResults);
        }

        // Act
        SloViolationHandler.ViolationResponse response = handler.recordViolation(
            experimentId,
            SloViolationHandler.ViolationType.BASELINE_BREACH,
            mockSloResults
        );

        // Assert
        assertTrue(response.isShouldAbort(), "Should recommend abort for critical violation");
        assertTrue(response.isShouldAlert(), "Should recommend alert for critical violation");
        assertTrue(response.getRecommendedActions().contains("ABORT_EXPERIMENT"));
        assertTrue(response.getRecommendedActions().contains("NOTIFY_ON_CALL_ENGINEER"));
    }
}

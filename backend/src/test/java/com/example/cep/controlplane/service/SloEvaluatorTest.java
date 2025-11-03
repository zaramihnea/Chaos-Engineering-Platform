package com.example.cep.controlplane.service;

import com.example.cep.integration.PrometheusClient;
import com.example.cep.model.SloMetric;
import com.example.cep.model.SloTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD Iteration 2: Test-Driven Development for SLO Evaluation
 *
 * This test suite validates the SLO (Service Level Objective) evaluation logic.
 * SLOs are critical for determining if a chaos experiment caused unacceptable
 * degradation to the system.
 *
 * Key Testing Scenarios:
 * - Querying Prometheus for metrics and extracting values
 * - Comparing metrics against thresholds using different comparators
 * - Detecting breaches when metrics violate SLO thresholds
 *
 * Test Coverage: 3 unit tests covering SLO evaluation and breach detection
 * Integration: Mocks PrometheusClient to simulate metric queries
 */
class SloEvaluatorTest {

    @Mock
    private PrometheusClient prometheusClient;

    private SloEvaluator sloEvaluator;

    /**
     * Setup method executed before each test
     * Initializes mocks and creates instance under test
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sloEvaluator = new SloEvaluatorImpl(prometheusClient);
    }

    /**
     * TDD Iteration 2 - Test 1: SLO Evaluation with Valid Metrics
     *
     * RED Phase: This test will fail because SloEvaluatorImpl doesn't exist
     * Expected failure: Class not found or method returns null/empty map
     */
    @Test
    @DisplayName("TDD-2.5: Evaluate valid SLOs returns metrics from Prometheus")
    void testEvaluate_ValidSlos_ReturnsMetrics() {
        // ARRANGE: Create SLO targets
        SloTarget latencySlo = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,  // 500ms threshold
            "<"
        );

        SloTarget errorRateSlo = new SloTarget(
            SloMetric.ERROR_RATE,
            "rate(http_requests_total{status=~\"5..\"}[5m])",
            0.01,  // 1% error rate threshold
            "<"
        );

        List<SloTarget> slos = Arrays.asList(latencySlo, errorRateSlo);

        // Mock Prometheus responses
        // Latency query response (350ms - good)
        Map<String, Object> latencyResponse = createPrometheusResponse(350.0);
        when(prometheusClient.queryInstant(latencySlo.getPromQuery())).thenReturn(latencyResponse);

        // Error rate query response (0.005 = 0.5% - good)
        Map<String, Object> errorRateResponse = createPrometheusResponse(0.005);
        when(prometheusClient.queryInstant(errorRateSlo.getPromQuery())).thenReturn(errorRateResponse);

        // ACT: Evaluate SLOs
        Map<String, Object> results = sloEvaluator.evaluate(slos);

        // ASSERT: Verify results contain metrics
        assertNotNull(results, "Results should not be null");
        assertFalse(results.isEmpty(), "Results should contain metric values");

        // Verify latency metric
        assertTrue(results.containsKey("latency_p95"), "Results should contain latency_p95 metric");
        assertEquals(350.0, results.get("latency_p95"), "Latency value should be 350.0");

        // Verify error rate metric
        assertTrue(results.containsKey("error_rate"), "Results should contain error_rate metric");
        assertEquals(0.005, results.get("error_rate"), "Error rate value should be 0.005");

        // Verify thresholds are included for breach detection
        assertTrue(results.containsKey("latency_p95_threshold"),
            "Results should include latency threshold");
        assertTrue(results.containsKey("error_rate_threshold"),
            "Results should include error rate threshold");

        // Verify Prometheus was queried for each SLO
        verify(prometheusClient, times(2)).queryInstant(anyString());
    }

    /**
     * TDD Iteration 2 - Test 2: Breach Detection when Threshold Exceeded
     *
     * RED Phase: This test will fail because breach detection logic is not implemented
     * Expected failure: Method returns false when it should return true
     */
    @Test
    @DisplayName("TDD-2.6: Breaches returns true when metric exceeds threshold")
    void testBreaches_ThresholdExceeded_ReturnsTrue() {
        // ARRANGE: Create evaluation results with a BREACHED metric
        Map<String, Object> results = new HashMap<>();

        // Latency is ABOVE threshold (BREACH for < comparator)
        results.put("latency_p95", 650.0);  // Actual value
        results.put("latency_p95_threshold", 500.0);  // Threshold
        results.put("latency_p95_comparator", "<");  // Should be less than

        // Error rate is ABOVE threshold (BREACH for < comparator)
        results.put("error_rate", 0.03);  // 3% error rate
        results.put("error_rate_threshold", 0.01);  // 1% threshold
        results.put("error_rate_comparator", "<");

        // ACT: Check for breaches
        boolean hasBreaches = sloEvaluator.breaches(results);

        // ASSERT: Verify breach is detected
        assertTrue(hasBreaches, "Should detect breach when metrics exceed thresholds");
    }

    /**
     * TDD Iteration 2 - Test 3: No Breach when Within Threshold
     *
     * RED Phase: This test will fail because breach logic may incorrectly flag good metrics
     * Expected failure: Method returns true when it should return false
     */
    @Test
    @DisplayName("TDD-2.7: Breaches returns false when metrics are within threshold")
    void testBreaches_WithinThreshold_ReturnsFalse() {
        // ARRANGE: Create evaluation results with GOOD metrics (no breach)
        Map<String, Object> results = new HashMap<>();

        // Latency is BELOW threshold (GOOD for < comparator)
        results.put("latency_p95", 350.0);  // Actual value
        results.put("latency_p95_threshold", 500.0);  // Threshold
        results.put("latency_p95_comparator", "<");  // Should be less than - SATISFIED

        // Error rate is BELOW threshold (GOOD for < comparator)
        results.put("error_rate", 0.005);  // 0.5% error rate
        results.put("error_rate_threshold", 0.01);  // 1% threshold
        results.put("error_rate_comparator", "<");  // Should be less than - SATISFIED

        // Availability is ABOVE threshold (GOOD for > comparator)
        results.put("availability", 0.999);  // 99.9% uptime
        results.put("availability_threshold", 0.99);  // 99% required
        results.put("availability_comparator", ">");  // Should be greater than - SATISFIED

        // ACT: Check for breaches
        boolean hasBreaches = sloEvaluator.breaches(results);

        // ASSERT: Verify no breach is detected
        assertFalse(hasBreaches, "Should not detect breach when all metrics are within thresholds");
    }

    /**
     * TDD Iteration 2 - Test 4: Test Different Comparators
     *
     * Bonus test to ensure all comparator types work correctly
     */
    @Test
    @DisplayName("TDD-2.8: Breaches correctly handles different comparator types")
    void testBreaches_DifferentComparators_HandledCorrectly() {
        // ARRANGE: Test each comparator type

        // Test 1: < comparator with breach
        Map<String, Object> lessThanBreach = new HashMap<>();
        lessThanBreach.put("metric", 100.0);
        lessThanBreach.put("metric_threshold", 50.0);
        lessThanBreach.put("metric_comparator", "<");
        assertTrue(sloEvaluator.breaches(lessThanBreach),
            "< comparator: Should breach when value >= threshold");

        // Test 2: < comparator without breach
        Map<String, Object> lessThanOk = new HashMap<>();
        lessThanOk.put("metric", 30.0);
        lessThanOk.put("metric_threshold", 50.0);
        lessThanOk.put("metric_comparator", "<");
        assertFalse(sloEvaluator.breaches(lessThanOk),
            "< comparator: Should not breach when value < threshold");

        // Test 3: > comparator with breach
        Map<String, Object> greaterThanBreach = new HashMap<>();
        greaterThanBreach.put("metric", 30.0);
        greaterThanBreach.put("metric_threshold", 50.0);
        greaterThanBreach.put("metric_comparator", ">");
        assertTrue(sloEvaluator.breaches(greaterThanBreach),
            "> comparator: Should breach when value <= threshold");

        // Test 4: > comparator without breach
        Map<String, Object> greaterThanOk = new HashMap<>();
        greaterThanOk.put("metric", 100.0);
        greaterThanOk.put("metric_threshold", 50.0);
        greaterThanOk.put("metric_comparator", ">");
        assertFalse(sloEvaluator.breaches(greaterThanOk),
            "> comparator: Should not breach when value > threshold");

        // Test 5: <= comparator
        Map<String, Object> lessOrEqualOk = new HashMap<>();
        lessOrEqualOk.put("metric", 50.0);  // Equal to threshold
        lessOrEqualOk.put("metric_threshold", 50.0);
        lessOrEqualOk.put("metric_comparator", "<=");
        assertFalse(sloEvaluator.breaches(lessOrEqualOk),
            "<= comparator: Should not breach when value == threshold");

        // Test 6: >= comparator
        Map<String, Object> greaterOrEqualOk = new HashMap<>();
        greaterOrEqualOk.put("metric", 50.0);  // Equal to threshold
        greaterOrEqualOk.put("metric_threshold", 50.0);
        greaterOrEqualOk.put("metric_comparator", ">=");
        assertFalse(sloEvaluator.breaches(greaterOrEqualOk),
            ">= comparator: Should not breach when value == threshold");
    }

    // ==================== Test Helper Methods ====================

    /**
     * Creates a mock Prometheus instant query response
     *
     * Prometheus instant query format:
     * {
     *   "data": {
     *     "result": [
     *       {
     *         "value": [timestamp, "value"]
     *       }
     *     ]
     *   }
     * }
     *
     * @param value The metric value to return
     * @return Mock Prometheus response map
     */
    private Map<String, Object> createPrometheusResponse(double value) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> resultItem = new HashMap<>();

        // Prometheus returns value as [timestamp, "value_string"]
        List<Object> valueArray = Arrays.asList(
            System.currentTimeMillis() / 1000.0,  // timestamp
            String.valueOf(value)  // value as string
        );

        resultItem.put("value", valueArray);
        result.add(resultItem);
        data.put("result", result);
        response.put("data", data);

        return response;
    }

    /**
     * Creates a mock Prometheus error response
     * Used for testing error handling scenarios
     */
    @SuppressWarnings("unused")
    private Map<String, Object> createPrometheusErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        return response;
    }
}

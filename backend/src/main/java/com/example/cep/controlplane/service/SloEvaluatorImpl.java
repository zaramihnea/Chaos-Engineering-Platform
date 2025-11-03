package com.example.cep.controlplane.service;

import com.example.cep.integration.PrometheusClient;
import com.example.cep.model.SloTarget;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SLO Evaluator Service Implementation
 *
 * TDD Iteration 2 - GREEN Phase Implementation
 *
 * This service evaluates Service Level Objectives (SLOs) by querying Prometheus
 * for metrics and comparing them against defined thresholds.
 *
 * SLO evaluation is critical for chaos engineering because it provides objective
 * measurement of experiment impact. Without SLO evaluation, we cannot determine
 * if an experiment caused acceptable or unacceptable degradation.
 *
 * SOA Principles Demonstrated:
 * - Service Abstraction: Hides Prometheus query complexity
 * - Service Reusability: Can evaluate SLOs for any experiment
 * - Loose Coupling: Depends only on PrometheusClient interface
 * - Single Responsibility: Focused only on SLO evaluation logic
 *
 * Comparator Support:
 * - "<"  : Breach if actual >= threshold (for latency, error rate)
 * - "<=" : Breach if actual > threshold
 * - ">"  : Breach if actual <= threshold (for throughput, availability)
 * - ">=" : Breach if actual < threshold
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0 (TDD Iteration 2)
 */
@Service
public class SloEvaluatorImpl implements SloEvaluator {

    private final PrometheusClient prometheusClient;

    /**
     * Constructor-based dependency injection
     *
     * @param prometheusClient Client for querying Prometheus metrics
     */
    public SloEvaluatorImpl(PrometheusClient prometheusClient) {
        this.prometheusClient = prometheusClient;
    }

    /**
     * Evaluates a list of SLO targets by querying Prometheus
     *
     * TDD Test Coverage:
     * - testEvaluate_ValidSlos_ReturnsMetrics (GREEN)
     *
     * Process:
     * 1. For each SLO target, query Prometheus using instant query
     * 2. Extract numeric value from Prometheus response
     * 3. Store metric value, threshold, and comparator in results map
     * 4. Handle errors gracefully by storing error messages
     *
     * Result Map Format:
     * {
     *   "latency_p95": 350.0,
     *   "latency_p95_threshold": 500.0,
     *   "latency_p95_comparator": "<",
     *   "error_rate": 0.005,
     *   "error_rate_threshold": 0.01,
     *   "error_rate_comparator": "<"
     * }
     *
     * @param slos List of SLO targets to evaluate
     * @return Map of metric names to values with thresholds and comparators
     */
    @Override
    public Map<String, Object> evaluate(List<SloTarget> slos) {
        Map<String, Object> results = new HashMap<>();

        if (slos == null || slos.isEmpty()) {
            System.out.println("Warning: No SLOs to evaluate");
            return results;
        }

        System.out.println("Evaluating " + slos.size() + " SLO targets...");

        for (SloTarget slo : slos) {
            try {
                // Step 1: Query Prometheus for this metric
                Map<String, Object> response = prometheusClient.queryInstant(slo.getPromQuery());

                // Step 2: Extract value from Prometheus response
                Double value = extractValue(response);

                // Step 3: Store in results map
                String metricKey = getMetricKey(slo);

                if (value != null) {
                    results.put(metricKey, value);
                    results.put(metricKey + "_threshold", slo.getThreshold());
                    results.put(metricKey + "_comparator", slo.getComparator());

                    System.out.println("  ✓ " + metricKey + ": " + value +
                                     " (threshold: " + slo.getComparator() + " " +
                                     slo.getThreshold() + ")");
                } else {
                    // Store error information
                    results.put(metricKey, "ERROR");
                    results.put(metricKey + "_error", "Failed to extract value from Prometheus response");
                    System.err.println("  ✗ " + metricKey + ": Failed to extract value");
                }

            } catch (Exception e) {
                // Handle errors gracefully
                String metricKey = getMetricKey(slo);
                results.put(metricKey, "ERROR");
                results.put(metricKey + "_error", e.getMessage());
                System.err.println("  ✗ " + metricKey + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Checks if any SLO thresholds were breached
     *
     * TDD Test Coverage:
     * - testBreaches_ThresholdExceeded_ReturnsTrue (GREEN)
     * - testBreaches_WithinThreshold_ReturnsFalse (GREEN)
     * - testBreaches_DifferentComparators_HandledCorrectly (GREEN)
     *
     * This method iterates through all metrics in the results map and checks
     * if any violated their threshold according to the comparator.
     *
     * Breach Logic:
     * - For "<" comparator: breach if value >= threshold
     * - For "<=" comparator: breach if value > threshold
     * - For ">" comparator: breach if value <= threshold
     * - For ">=" comparator: breach if value < threshold
     *
     * @param results Map of evaluation results from evaluate()
     * @return true if any metric breached its threshold
     */
    @Override
    public boolean breaches(Map<String, Object> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }

        // Iterate through results to find metrics with thresholds
        for (String key : results.keySet()) {
            // Skip non-metric keys (thresholds, comparators, errors, etc.)
            if (key.contains("_threshold") || key.contains("_comparator") ||
                key.contains("_error") || key.equals("breach_detected") ||
                key.equals("breach_reason") || key.equals("original_outcome")) {
                continue;
            }

            // Get metric value
            Object valueObj = results.get(key);
            if (!(valueObj instanceof Number)) {
                continue;  // Skip non-numeric values (errors, etc.)
            }
            double value = ((Number) valueObj).doubleValue();

            // Get threshold and comparator
            Object thresholdObj = results.get(key + "_threshold");
            Object comparatorObj = results.get(key + "_comparator");

            if (thresholdObj == null || comparatorObj == null) {
                continue;  // Skip if missing threshold or comparator
            }

            double threshold = ((Number) thresholdObj).doubleValue();
            String comparator = comparatorObj.toString();

            // Check for breach based on comparator
            boolean breached = isBreached(value, threshold, comparator);

            if (breached) {
                System.out.println("⚠️  SLO breach detected: " + key +
                                 " = " + value + " (threshold: " + comparator + " " + threshold + ")");
                return true;  // Return immediately on first breach
            }
        }

        // No breaches found
        return false;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Extracts numeric value from Prometheus instant query response
     *
     * Prometheus Response Format:
     * {
     *   "data": {
     *     "result": [
     *       {
     *         "value": [timestamp, "value_as_string"]
     *       }
     *     ]
     *   }
     * }
     *
     * @param response Prometheus query response
     * @return Extracted numeric value, or null if extraction failed
     */
    @SuppressWarnings("unchecked")
    private Double extractValue(Map<String, Object> response) {
        try {
            // Navigate response structure
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return null;
            }

            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
            if (result == null || result.isEmpty()) {
                return null;
            }

            // Get first result item
            Map<String, Object> firstResult = result.get(0);
            List<Object> valueArray = (List<Object>) firstResult.get("value");

            if (valueArray == null || valueArray.size() < 2) {
                return null;
            }

            // Value is at index 1, as a string
            Object valueObj = valueArray.get(1);
            if (valueObj instanceof String) {
                return Double.parseDouble((String) valueObj);
            } else if (valueObj instanceof Number) {
                return ((Number) valueObj).doubleValue();
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error extracting value from Prometheus response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a metric key from SLO target
     * Converts SloMetric enum to lowercase string (e.g., LATENCY_P95 -> latency_p95)
     *
     * @param slo SLO target
     * @return Metric key for results map
     */
    private String getMetricKey(SloTarget slo) {
        return slo.getMetric().toString().toLowerCase();
    }

    /**
     * Determines if a metric value breaches its threshold
     *
     * Breach Logic:
     * - "<"  : Value must be less than threshold. Breach if value >= threshold.
     * - "<=" : Value must be less than or equal. Breach if value > threshold.
     * - ">"  : Value must be greater than threshold. Breach if value <= threshold.
     * - ">=" : Value must be greater than or equal. Breach if value < threshold.
     *
     * @param value Actual metric value
     * @param threshold SLO threshold
     * @param comparator Comparison operator
     * @return true if metric breached threshold
     */
    private boolean isBreached(double value, double threshold, String comparator) {
        switch (comparator) {
            case "<":
                // Value should be less than threshold
                // Breach if value >= threshold
                return value >= threshold;

            case "<=":
                // Value should be less than or equal to threshold
                // Breach if value > threshold
                return value > threshold;

            case ">":
                // Value should be greater than threshold
                // Breach if value <= threshold
                return value <= threshold;

            case ">=":
                // Value should be greater than or equal to threshold
                // Breach if value < threshold
                return value < threshold;

            default:
                System.err.println("Unknown comparator: " + comparator + ". Treating as no breach.");
                return false;
        }
    }

    // ==================== Future Enhancement Methods ====================

    /**
     * Future: Calculate percentage change from baseline
     * Would compare current value to historical baseline
     */
    @SuppressWarnings("unused")
    private double calculateDelta(double current, double baseline) {
        if (baseline == 0) {
            return 0;
        }
        return ((current - baseline) / baseline) * 100.0;
    }

    /**
     * Future: Support for range queries (time series)
     * Would allow trending analysis and anomaly detection
     */
    @SuppressWarnings("unused")
    private List<Double> evaluateTimeSeries(String promQuery, long startTime, long endTime) {
        // TODO: Implement time series evaluation
        // return prometheusClient.queryRange(promQuery, startTime, endTime, 60);
        return null;
    }
}

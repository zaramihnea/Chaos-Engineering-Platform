package com.example.cep.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SloTarget Model Tests")
class SloTargetTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void testConstructor() {
        SloTarget slo = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,
            "<"
        );

        assertEquals(SloMetric.LATENCY_P95, slo.getMetric());
        assertEquals("histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))", slo.getPromQuery());
        assertEquals(500.0, slo.getThreshold());
        assertEquals("<", slo.getComparator());
    }

    @Test
    @DisplayName("handles all SLO metrics")
    void testAllSloMetrics() {
        for (SloMetric metric : SloMetric.values()) {
            SloTarget slo = new SloTarget(metric, "query", 100.0, "<");
            assertEquals(metric, slo.getMetric());
        }
    }

    @Test
    @DisplayName("handles different comparators")
    void testComparators() {
        SloTarget lessThan = new SloTarget(SloMetric.LATENCY_P95, "query1", 500.0, "<");
        SloTarget greaterThan = new SloTarget(SloMetric.THROUGHPUT, "query2", 1000.0, ">");
        SloTarget equal = new SloTarget(SloMetric.AVAILABILITY, "query3", 99.9, "==");

        assertEquals("<", lessThan.getComparator());
        assertEquals(">", greaterThan.getComparator());
        assertEquals("==", equal.getComparator());
    }

    @Test
    @DisplayName("handles different threshold values")
    void testThresholds() {
        SloTarget slo1 = new SloTarget(SloMetric.LATENCY_P95, "query", 100.0, "<");
        SloTarget slo2 = new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<");
        SloTarget slo3 = new SloTarget(SloMetric.AVAILABILITY, "query", 99.99, ">");

        assertEquals(100.0, slo1.getThreshold());
        assertEquals(0.01, slo2.getThreshold());
        assertEquals(99.99, slo3.getThreshold());
    }

    @Test
    @DisplayName("handles complex Prometheus queries")
    void testComplexQueries() {
        String complexQuery = "sum(rate(http_requests_total{job=\"api-server\",code!~\"5..\"}[5m])) / sum(rate(http_requests_total{job=\"api-server\"}[5m]))";

        SloTarget slo = new SloTarget(SloMetric.ERROR_RATE, complexQuery, 0.001, "<");

        assertEquals(complexQuery, slo.getPromQuery());
    }

    @Test
    @DisplayName("handles all comparator types")
    void testAllComparatorTypes() {
        String[] comparators = {"<", ">", "<=", ">=", "==", "!="};

        for (String comparator : comparators) {
            SloTarget slo = new SloTarget(SloMetric.LATENCY_P95, "query", 100.0, comparator);
            assertEquals(comparator, slo.getComparator());
        }
    }

    @Test
    @DisplayName("handles different SLO metric combinations")
    void testMetricCombinations() {
        SloTarget latency = new SloTarget(SloMetric.LATENCY_P95, "query1", 500.0, "<");
        SloTarget errors = new SloTarget(SloMetric.ERROR_RATE, "query2", 0.01, "<");
        SloTarget availability = new SloTarget(SloMetric.AVAILABILITY, "query3", 99.9, ">");
        SloTarget throughput = new SloTarget(SloMetric.THROUGHPUT, "query4", 1000.0, ">");

        assertEquals(SloMetric.LATENCY_P95, latency.getMetric());
        assertEquals(SloMetric.ERROR_RATE, errors.getMetric());
        assertEquals(SloMetric.AVAILABILITY, availability.getMetric());
        assertEquals(SloMetric.THROUGHPUT, throughput.getMetric());
    }
}

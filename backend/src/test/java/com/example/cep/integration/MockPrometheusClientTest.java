package com.example.cep.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mock Prometheus Client Tests")
class MockPrometheusClientTest {

    private MockPrometheusClient client;

    @BeforeEach
    void setUp() {
        client = new MockPrometheusClient();
    }

    @Test
    @DisplayName("queryRange returns valid response structure")
    void testQueryRangeStructure() {
        Map<String, Object> response = client.queryRange(
            "up{job='prometheus'}",
            System.currentTimeMillis() / 1000 - 3600,
            System.currentTimeMillis() / 1000,
            60
        );

        assertNotNull(response);
        assertEquals("success", response.get("status"));
        assertTrue(response.containsKey("data"));

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data);
        assertTrue(data.containsKey("result"));

        List<?> result = (List<?>) data.get("result");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("queryRange handles different time ranges")
    void testQueryRangeDifferentTimeRanges() {
        long now = System.currentTimeMillis() / 1000;

        // Short time range
        Map<String, Object> response1 = client.queryRange("metric1", now - 300, now, 30);
        assertNotNull(response1);
        assertEquals("success", response1.get("status"));

        // Long time range
        Map<String, Object> response2 = client.queryRange("metric2", now - 86400, now, 300);
        assertNotNull(response2);
        assertEquals("success", response2.get("status"));

        // Very short time range
        Map<String, Object> response3 = client.queryRange("metric3", now - 60, now, 10);
        assertNotNull(response3);
        assertEquals("success", response3.get("status"));
    }

    @Test
    @DisplayName("queryRange handles various PromQL queries")
    void testQueryRangeVariousQueries() {
        long now = System.currentTimeMillis() / 1000;

        // Simple metric
        Map<String, Object> response1 = client.queryRange("up", now - 300, now, 30);
        assertNotNull(response1);
        assertEquals("success", response1.get("status"));

        // Complex query with functions
        Map<String, Object> response2 = client.queryRange(
            "rate(http_requests_total[5m])",
            now - 300,
            now,
            30
        );
        assertNotNull(response2);
        assertEquals("success", response2.get("status"));

        // Query with labels
        Map<String, Object> response3 = client.queryRange(
            "http_requests_total{job='api-server',status='200'}",
            now - 300,
            now,
            30
        );
        assertNotNull(response3);
        assertEquals("success", response3.get("status"));
    }

    @Test
    @DisplayName("queryInstant returns valid response structure")
    void testQueryInstantStructure() {
        Map<String, Object> response = client.queryInstant("up{job='prometheus'}");

        assertNotNull(response);
        assertEquals("success", response.get("status"));
        assertTrue(response.containsKey("data"));

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data);
        assertTrue(data.containsKey("result"));

        List<?> result = (List<?>) data.get("result");
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("queryInstant returns valid metric value")
    void testQueryInstantValue() {
        Map<String, Object> response = client.queryInstant("test_metric");

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, Object> resultItem = result.get(0);
        assertTrue(resultItem.containsKey("value"));

        List<Object> valueArray = (List<Object>) resultItem.get("value");
        assertNotNull(valueArray);
        assertEquals(2, valueArray.size());

        // Check timestamp
        assertTrue(valueArray.get(0) instanceof Number);

        // Check value
        assertEquals("0.001", valueArray.get(1));
    }

    @Test
    @DisplayName("queryInstant handles various PromQL queries")
    void testQueryInstantVariousQueries() {
        // Simple metric
        Map<String, Object> response1 = client.queryInstant("up");
        assertNotNull(response1);
        assertEquals("success", response1.get("status"));

        // Rate query
        Map<String, Object> response2 = client.queryInstant("rate(http_requests_total[5m])");
        assertNotNull(response2);
        assertEquals("success", response2.get("status"));

        // Aggregation query
        Map<String, Object> response3 = client.queryInstant("sum(rate(http_requests_total[5m]))");
        assertNotNull(response3);
        assertEquals("success", response3.get("status"));

        // Query with labels
        Map<String, Object> response4 = client.queryInstant("http_requests_total{status='200'}");
        assertNotNull(response4);
        assertEquals("success", response4.get("status"));
    }

    @Test
    @DisplayName("queryInstant value is below typical SLO thresholds")
    void testQueryInstantValueBelowThresholds() {
        Map<String, Object> response = client.queryInstant("latency_p95");

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
        Map<String, Object> resultItem = result.get(0);
        List<Object> valueArray = (List<Object>) resultItem.get("value");

        double value = Double.parseDouble((String) valueArray.get(1));

        // Mock value should be low enough for typical SLOs
        assertTrue(value < 1.0, "Mock value should be < 1.0");
        assertTrue(value < 0.01, "Mock value should be < 0.01 (1%)");
        assertEquals(0.001, value, 0.0001, "Mock value should be 0.001");
    }

    @Test
    @DisplayName("queryRange returns empty result list")
    void testQueryRangeEmptyResult() {
        Map<String, Object> response = client.queryRange(
            "some_metric",
            0,
            100,
            10
        );

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<?> result = (List<?>) data.get("result");

        assertTrue(result.isEmpty(), "queryRange should return empty result list");
    }

    @Test
    @DisplayName("queryInstant timestamp is recent")
    void testQueryInstantTimestamp() {
        long beforeQuery = System.currentTimeMillis() / 1000;
        Map<String, Object> response = client.queryInstant("test_metric");
        long afterQuery = System.currentTimeMillis() / 1000;

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
        Map<String, Object> resultItem = result.get(0);
        List<Object> valueArray = (List<Object>) resultItem.get("value");

        double timestamp = (Double) valueArray.get(0);

        assertTrue(timestamp >= beforeQuery, "Timestamp should be after query start");
        assertTrue(timestamp <= afterQuery + 1, "Timestamp should be before query end");
    }

    @Test
    @DisplayName("multiple queryInstant calls return consistent structure")
    void testMultipleQueryInstantCalls() {
        Map<String, Object> response1 = client.queryInstant("metric1");
        Map<String, Object> response2 = client.queryInstant("metric2");
        Map<String, Object> response3 = client.queryInstant("metric3");

        // All should have success status
        assertEquals("success", response1.get("status"));
        assertEquals("success", response2.get("status"));
        assertEquals("success", response3.get("status"));

        // All should have same value
        Map<String, Object> data1 = (Map<String, Object>) response1.get("data");
        Map<String, Object> data2 = (Map<String, Object>) response2.get("data");
        Map<String, Object> data3 = (Map<String, Object>) response3.get("data");

        List<Map<String, Object>> result1 = (List<Map<String, Object>>) data1.get("result");
        List<Map<String, Object>> result2 = (List<Map<String, Object>>) data2.get("result");
        List<Map<String, Object>> result3 = (List<Map<String, Object>>) data3.get("result");

        List<Object> value1 = (List<Object>) result1.get(0).get("value");
        List<Object> value2 = (List<Object>) result2.get(0).get("value");
        List<Object> value3 = (List<Object>) result3.get(0).get("value");

        assertEquals(value1.get(1), value2.get(1));
        assertEquals(value2.get(1), value3.get(1));
    }

    @Test
    @DisplayName("multiple queryRange calls return consistent structure")
    void testMultipleQueryRangeCalls() {
        long now = System.currentTimeMillis() / 1000;

        Map<String, Object> response1 = client.queryRange("metric1", now - 300, now, 30);
        Map<String, Object> response2 = client.queryRange("metric2", now - 600, now, 60);
        Map<String, Object> response3 = client.queryRange("metric3", now - 120, now, 10);

        // All should have success status
        assertEquals("success", response1.get("status"));
        assertEquals("success", response2.get("status"));
        assertEquals("success", response3.get("status"));

        // All should have empty results
        Map<String, Object> data1 = (Map<String, Object>) response1.get("data");
        Map<String, Object> data2 = (Map<String, Object>) response2.get("data");
        Map<String, Object> data3 = (Map<String, Object>) response3.get("data");

        List<?> result1 = (List<?>) data1.get("result");
        List<?> result2 = (List<?>) data2.get("result");
        List<?> result3 = (List<?>) data3.get("result");

        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
        assertTrue(result3.isEmpty());
    }
}

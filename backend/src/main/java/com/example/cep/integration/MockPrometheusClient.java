package com.example.cep.integration;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mock implementation of PrometheusClient
 * This is a temporary implementation for testing the Spring context
 * In production, this would be replaced with actual Prometheus HTTP client
 */
@Component
public class MockPrometheusClient implements PrometheusClient {

    @Override
    public Map<String, Object> queryRange(String promQl, long startEpochSec, long endEpochSec, long stepSec) {
        // Return empty mock response
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("result", new ArrayList<>());
        response.put("data", data);
        response.put("status", "success");
        return response;
    }

    @Override
    public Map<String, Object> queryInstant(String promQl) {
        // Return mock response with dummy metric value
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> resultItem = new HashMap<>();

        // Prometheus returns value as [timestamp, "value_string"]
        List<Object> valueArray = Arrays.asList(
            System.currentTimeMillis() / 1000.0,  // timestamp
            "100.0"  // mock value
        );

        resultItem.put("value", valueArray);
        result.add(resultItem);
        data.put("result", result);
        response.put("data", data);
        response.put("status", "success");
        return response;
    }
}

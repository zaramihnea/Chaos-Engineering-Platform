package com.example.cep.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TargetSystem Model Tests")
class TargetSystemTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void testConstructor() {
        Map<String, String> labels = Map.of("app", "cart-service", "env", "prod");
        TargetSystem target = new TargetSystem("prod-cluster", "default", labels);

        assertEquals("prod-cluster", target.getCluster());
        assertEquals("default", target.getNamespace());
        assertEquals(labels, target.getLabels());
    }

    @Test
    @DisplayName("getLabels returns correct map")
    void testGetLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", "payment-service");
        labels.put("version", "v2.1.0");
        labels.put("team", "backend");

        TargetSystem target = new TargetSystem("staging-cluster", "staging", labels);

        assertEquals(3, target.getLabels().size());
        assertEquals("payment-service", target.getLabels().get("app"));
        assertEquals("v2.1.0", target.getLabels().get("version"));
        assertEquals("backend", target.getLabels().get("team"));
    }

    @Test
    @DisplayName("handles empty labels map")
    void testEmptyLabels() {
        TargetSystem target = new TargetSystem("cluster", "namespace", Map.of());

        assertNotNull(target.getLabels());
        assertTrue(target.getLabels().isEmpty());
    }

    @Test
    @DisplayName("handles different cluster names")
    void testDifferentClusters() {
        TargetSystem prod = new TargetSystem("prod-us-east-1", "default", Map.of());
        TargetSystem staging = new TargetSystem("staging-eu-west-1", "default", Map.of());
        TargetSystem dev = new TargetSystem("dev-local", "default", Map.of());

        assertEquals("prod-us-east-1", prod.getCluster());
        assertEquals("staging-eu-west-1", staging.getCluster());
        assertEquals("dev-local", dev.getCluster());
    }

    @Test
    @DisplayName("handles different namespaces")
    void testDifferentNamespaces() {
        TargetSystem defaultNs = new TargetSystem("cluster", "default", Map.of());
        TargetSystem customNs = new TargetSystem("cluster", "payment-service", Map.of());
        TargetSystem systemNs = new TargetSystem("cluster", "kube-system", Map.of());

        assertEquals("default", defaultNs.getNamespace());
        assertEquals("payment-service", customNs.getNamespace());
        assertEquals("kube-system", systemNs.getNamespace());
    }
}

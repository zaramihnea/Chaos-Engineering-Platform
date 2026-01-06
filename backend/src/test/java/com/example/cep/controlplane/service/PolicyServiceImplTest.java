package com.example.cep.controlplane.service;

import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.FaultType;
import com.example.cep.model.SloMetric;
import com.example.cep.model.SloTarget;
import com.example.cep.model.TargetSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Policy Service Implementation Tests")
class PolicyServiceImplTest {

    private PolicyServiceImpl policyService;
    private ExperimentDefinition validDefinition;

    @BeforeEach
    void setUp() {
        policyService = new PolicyServiceImpl();

        // Create a valid experiment definition
        TargetSystem target = new TargetSystem(
            "production-cluster",
            "default",
            Map.of("app", "test")
        );

        SloTarget sloTarget = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,
            "<"
        );

        validDefinition = new ExperimentDefinition(
            "test-exp-001",
            "Test Experiment",
            FaultType.POD_KILL,
            new HashMap<>(),
            target,
            Duration.ofMinutes(10),
            List.of(sloTarget),
            true,
            "test-user"
        );
    }

    @Test
    @DisplayName("isAllowed returns true for valid experiment")
    void testIsAllowed_ValidExperiment() {
        assertTrue(policyService.isAllowed(validDefinition));
    }

    @Test
    @DisplayName("isAllowed returns false for null definition")
    void testIsAllowed_NullDefinition() {
        assertFalse(policyService.isAllowed(null));
    }

    @Test
    @DisplayName("isAllowed returns false for null target")
    void testIsAllowed_NullTarget() {
        ExperimentDefinition defWithNullTarget = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            null,
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(defWithNullTarget));
    }

    @Test
    @DisplayName("isAllowed returns false for invalid namespace")
    void testIsAllowed_InvalidNamespace() {
        TargetSystem invalidTarget = new TargetSystem(
            "production-cluster",
            "production",  // Not in allowed list
            Map.of()
        );

        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            invalidTarget,
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns true for all allowed namespaces")
    void testIsAllowed_AllAllowedNamespaces() {
        String[] allowedNamespaces = {"default", "staging", "test", "dev"};

        for (String namespace : allowedNamespaces) {
            TargetSystem target = new TargetSystem(
                "production-cluster",
                namespace,
                Map.of()
            );

            ExperimentDefinition def = new ExperimentDefinition(
                "exp-" + namespace,
                "Test " + namespace,
                FaultType.POD_KILL,
                new HashMap<>(),
                target,
                Duration.ofMinutes(5),
                List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
                true,
                "user"
            );

            assertTrue(policyService.isAllowed(def), "Should allow namespace: " + namespace);
        }
    }

    @Test
    @DisplayName("isAllowed returns false for invalid cluster")
    void testIsAllowed_InvalidCluster() {
        TargetSystem invalidTarget = new TargetSystem(
            "invalid-cluster",
            "default",
            Map.of()
        );

        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            invalidTarget,
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns true for all allowed clusters")
    void testIsAllowed_AllAllowedClusters() {
        String[] allowedClusters = {"production-cluster", "staging-cluster", "dev-cluster"};

        for (String cluster : allowedClusters) {
            TargetSystem target = new TargetSystem(
                cluster,
                "default",
                Map.of()
            );

            ExperimentDefinition def = new ExperimentDefinition(
                "exp-" + cluster,
                "Test " + cluster,
                FaultType.POD_KILL,
                new HashMap<>(),
                target,
                Duration.ofMinutes(5),
                List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
                true,
                "user"
            );

            assertTrue(policyService.isAllowed(def), "Should allow cluster: " + cluster);
        }
    }

    @Test
    @DisplayName("isAllowed returns false for duration exceeding limit")
    void testIsAllowed_DurationTooLong() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(31),  // Exceeds 30-minute limit
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns false for null duration")
    void testIsAllowed_NullDuration() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            null,
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns false for zero duration")
    void testIsAllowed_ZeroDuration() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofSeconds(0),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns true for duration at limit")
    void testIsAllowed_DurationAtLimit() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(30),  // Exactly at limit
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        assertTrue(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns false for insufficient SLOs")
    void testIsAllowed_InsufficientSlos() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(5),
            new ArrayList<>(),  // Empty SLO list
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("isAllowed returns false for null SLO list")
    void testIsAllowed_NullSlos() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(5),
            null,
            true,
            "user"
        );

        assertFalse(policyService.isAllowed(def));
    }

    @Test
    @DisplayName("denialReason returns empty string for valid experiment")
    void testDenialReason_ValidExperiment() {
        String reason = policyService.denialReason(validDefinition);
        assertEquals("", reason);
    }

    @Test
    @DisplayName("denialReason returns message for null definition")
    void testDenialReason_NullDefinition() {
        String reason = policyService.denialReason(null);
        assertTrue(reason.contains("null"));
    }

    @Test
    @DisplayName("denialReason returns message for null target")
    void testDenialReason_NullTarget() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            null,
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("Target"));
    }

    @Test
    @DisplayName("denialReason explains invalid namespace")
    void testDenialReason_InvalidNamespace() {
        TargetSystem target = new TargetSystem(
            "production-cluster",
            "production",
            Map.of()
        );

        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            target,
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("namespace"));
        assertTrue(reason.contains("production"));
    }

    @Test
    @DisplayName("denialReason explains invalid cluster")
    void testDenialReason_InvalidCluster() {
        TargetSystem target = new TargetSystem(
            "invalid-cluster",
            "default",
            Map.of()
        );

        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            target,
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("cluster"));
        assertTrue(reason.contains("invalid-cluster"));
    }

    @Test
    @DisplayName("denialReason explains duration exceeded")
    void testDenialReason_DurationExceeded() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(31),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("duration"));
        assertTrue(reason.contains("1860"));  // 31 minutes in seconds
    }

    @Test
    @DisplayName("denialReason explains insufficient SLOs")
    void testDenialReason_InsufficientSlos() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(5),
            new ArrayList<>(),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("SLO"));
        assertTrue(reason.contains("0"));
    }

    @Test
    @DisplayName("denialReason explains restricted fault type")
    void testDenialReason_RestrictedFaultType() {
        ExperimentDefinition def = new ExperimentDefinition(
            "exp-001",
            "Test",
            FaultType.NETWORK_PARTITION,  // Restricted fault type
            new HashMap<>(),
            new TargetSystem("production-cluster", "default", Map.of()),
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
            true,
            "user"
        );

        String reason = policyService.denialReason(def);
        assertTrue(reason.contains("approval"));
        assertTrue(reason.contains("NETWORK_PARTITION"));
    }

    @Test
    @DisplayName("isAllowed accepts non-restricted fault types")
    void testIsAllowed_NonRestrictedFaultTypes() {
        FaultType[] nonRestricted = {
            FaultType.POD_KILL,
            FaultType.NETWORK_DELAY,
            FaultType.CPU_STRESS,
            FaultType.MEMORY_STRESS
        };

        for (FaultType faultType : nonRestricted) {
            ExperimentDefinition def = new ExperimentDefinition(
                "exp-" + faultType,
                "Test " + faultType,
                faultType,
                new HashMap<>(),
                new TargetSystem("production-cluster", "default", Map.of()),
                Duration.ofMinutes(5),
                List.of(new SloTarget(SloMetric.ERROR_RATE, "query", 0.01, "<")),
                true,
                "user"
            );

            assertTrue(policyService.isAllowed(def), "Should allow fault type: " + faultType);
        }
    }
}

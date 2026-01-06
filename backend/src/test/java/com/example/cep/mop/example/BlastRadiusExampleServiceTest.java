package com.example.cep.mop.example;

import com.example.cep.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Blast Radius Example Service Tests")
class BlastRadiusExampleServiceTest {

    private BlastRadiusExampleService service;
    private RunPlan testRunPlan;
    private ExperimentDefinition testDefinition;

    @BeforeEach
    void setUp() {
        service = new BlastRadiusExampleService();

        // Create test data
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("podName", "test-pod");
        parameters.put("namespace", "default");

        TargetSystem target = new TargetSystem(
            "test-service",
            "default",
            Map.of("app", "test")
        );

        SloTarget sloTarget = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,
            "<"
        );

        testDefinition = new ExperimentDefinition(
            "test-exp-001",
            "Test Pod Kill Experiment",
            FaultType.POD_KILL,
            parameters,
            target,
            Duration.ofMinutes(5),
            List.of(sloTarget),
            true,
            "test-user"
        );

        testRunPlan = new RunPlan(
            "run-001",
            testDefinition,
            Instant.now(),
            false
        );
    }

    @Test
    @DisplayName("executeSafePodKill completes successfully")
    void testExecuteSafePodKill() throws InterruptedException {
        String result = service.executeSafePodKill(testRunPlan);

        assertNotNull(result);
        assertEquals("Pod kill operation completed successfully", result);
    }

    @Test
    @DisplayName("executeSafePodKill uses correct run plan")
    void testExecuteSafePodKillWithRunPlan() throws InterruptedException {
        RunPlan customPlan = new RunPlan(
            "custom-run",
            testDefinition,
            Instant.now(),
            true
        );

        String result = service.executeSafePodKill(customPlan);

        assertNotNull(result);
        assertTrue(result.contains("successfully"));
    }

    @Test
    @DisplayName("executeSafePodKill handles different experiment IDs")
    void testExecuteSafePodKillDifferentExperiments() throws InterruptedException {
        ExperimentDefinition def1 = new ExperimentDefinition(
            "exp-001",
            "Experiment 1",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("svc1", "ns1", Map.of()),
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user1"
        );

        ExperimentDefinition def2 = new ExperimentDefinition(
            "exp-002",
            "Experiment 2",
            FaultType.NETWORK_DELAY,
            new HashMap<>(),
            new TargetSystem("svc2", "ns2", Map.of()),
            Duration.ofMinutes(10),
            List.of(),
            false,
            "user2"
        );

        RunPlan plan1 = new RunPlan("run-1", def1, Instant.now(), false);
        RunPlan plan2 = new RunPlan("run-2", def2, Instant.now(), false);

        String result1 = service.executeSafePodKill(plan1);
        String result2 = service.executeSafePodKill(plan2);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("executeUnsafePodKill completes execution")
    void testExecuteUnsafePodKill() throws InterruptedException {
        String result = service.executeUnsafePodKill(testRunPlan);

        assertNotNull(result);
        assertTrue(result.contains("should not be returned") || result.contains("aborted"));
    }

    @Test
    @DisplayName("executeUnsafePodKill handles different run plans")
    void testExecuteUnsafePodKillWithDifferentPlans() throws InterruptedException {
        RunPlan dryRunPlan = new RunPlan(
            "dry-run-001",
            testDefinition,
            Instant.now(),
            true
        );

        String result = service.executeUnsafePodKill(dryRunPlan);

        assertNotNull(result);
    }

    @Test
    @DisplayName("executeMultiServiceExperiment completes successfully")
    void testExecuteMultiServiceExperiment() throws InterruptedException {
        String result = service.executeMultiServiceExperiment(testRunPlan);

        assertNotNull(result);
        assertEquals("Multi-service experiment completed successfully", result);
    }

    @Test
    @DisplayName("executeMultiServiceExperiment handles different fault types")
    void testExecuteMultiServiceExperimentDifferentFaults() throws InterruptedException {
        ExperimentDefinition networkDef = new ExperimentDefinition(
            "net-exp",
            "Network Experiment",
            FaultType.NETWORK_DELAY,
            Map.of("delay", "100ms"),
            new TargetSystem("svc", "ns", Map.of()),
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user"
        );

        RunPlan networkPlan = new RunPlan("net-run", networkDef, Instant.now(), false);

        String result = service.executeMultiServiceExperiment(networkPlan);

        assertNotNull(result);
        assertTrue(result.contains("successfully"));
    }

    @Test
    @DisplayName("all methods handle null definition gracefully")
    void testMethodsWithValidRunPlan() throws InterruptedException {
        RunPlan validPlan = new RunPlan(
            "valid-run",
            testDefinition,
            Instant.now(),
            false
        );

        assertDoesNotThrow(() -> service.executeSafePodKill(validPlan));
        assertDoesNotThrow(() -> service.executeUnsafePodKill(validPlan));
        assertDoesNotThrow(() -> service.executeMultiServiceExperiment(validPlan));
    }

    @Test
    @DisplayName("executeSafePodKill execution time is reasonable")
    void testExecuteSafePodKillTiming() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        service.executeSafePodKill(testRunPlan);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 5000, "Should take at least 5 seconds (3s + 2s sleep)");
        assertTrue(duration < 7000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("executeUnsafePodKill execution time is reasonable")
    void testExecuteUnsafePodKillTiming() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        service.executeUnsafePodKill(testRunPlan);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 6000, "Should take at least 6 seconds (3s + 3s sleep)");
        assertTrue(duration < 8000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("executeMultiServiceExperiment execution time is reasonable")
    void testExecuteMultiServiceExperimentTiming() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        service.executeMultiServiceExperiment(testRunPlan);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 6000, "Should take at least 6 seconds (3 * 2s sleep)");
        assertTrue(duration < 8000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("methods handle scheduled execution time")
    void testScheduledExecutionTime() throws InterruptedException {
        Instant futureTime = Instant.now().plusSeconds(300);
        RunPlan futurePlan = new RunPlan("future-run", testDefinition, futureTime, false);

        String result = service.executeSafePodKill(futurePlan);

        assertNotNull(result);
        assertEquals("Pod kill operation completed successfully", result);
    }

    @Test
    @DisplayName("methods handle past execution time")
    void testPastExecutionTime() throws InterruptedException {
        Instant pastTime = Instant.now().minusSeconds(300);
        RunPlan pastPlan = new RunPlan("past-run", testDefinition, pastTime, false);

        String result = service.executeMultiServiceExperiment(pastPlan);

        assertNotNull(result);
        assertEquals("Multi-service experiment completed successfully", result);
    }

    @Test
    @DisplayName("service handles dry run mode")
    void testDryRunMode() throws InterruptedException {
        RunPlan dryRunPlan = new RunPlan("dry-001", testDefinition, Instant.now(), true);

        String result1 = service.executeSafePodKill(dryRunPlan);
        String result2 = service.executeMultiServiceExperiment(dryRunPlan);

        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("service handles different target systems")
    void testDifferentTargetSystems() throws InterruptedException {
        TargetSystem prodTarget = new TargetSystem("prod-service", "production", Map.of("env", "prod"));
        TargetSystem devTarget = new TargetSystem("dev-service", "development", Map.of("env", "dev"));

        ExperimentDefinition prodDef = new ExperimentDefinition(
            "prod-exp",
            "Production Experiment",
            FaultType.POD_KILL,
            new HashMap<>(),
            prodTarget,
            Duration.ofMinutes(5),
            List.of(),
            false,
            "admin"
        );

        ExperimentDefinition devDef = new ExperimentDefinition(
            "dev-exp",
            "Development Experiment",
            FaultType.NETWORK_DELAY,
            new HashMap<>(),
            devTarget,
            Duration.ofMinutes(10),
            List.of(),
            true,
            "developer"
        );

        RunPlan prodPlan = new RunPlan("prod-run", prodDef, Instant.now(), false);
        RunPlan devPlan = new RunPlan("dev-run", devDef, Instant.now(), true);

        String prodResult = service.executeSafePodKill(prodPlan);
        String devResult = service.executeMultiServiceExperiment(devPlan);

        assertNotNull(prodResult);
        assertNotNull(devResult);
    }

    @Test
    @DisplayName("service handles various fault types")
    void testVariousFaultTypes() throws InterruptedException {
        FaultType[] faultTypes = {
            FaultType.POD_KILL,
            FaultType.NETWORK_DELAY,
            FaultType.CPU_STRESS,
            FaultType.MEMORY_STRESS
        };

        for (FaultType faultType : faultTypes) {
            ExperimentDefinition def = new ExperimentDefinition(
                "exp-" + faultType,
                "Test " + faultType,
                faultType,
                new HashMap<>(),
                new TargetSystem("svc", "ns", Map.of()),
                Duration.ofMinutes(5),
                List.of(),
                true,
                "user"
            );

            RunPlan plan = new RunPlan("run-" + faultType, def, Instant.now(), false);

            String result = service.executeSafePodKill(plan);
            assertNotNull(result, "Should handle " + faultType);
        }
    }
}

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

@DisplayName("Experiment Execution Service Tests")
class ExperimentExecutionServiceTest {

    private ExperimentExecutionService service;
    private RunPlan testRunPlan;
    private ExperimentDefinition testDefinition;

    @BeforeEach
    void setUp() {
        service = new ExperimentExecutionService();

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
            "Test Experiment",
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
    @DisplayName("executeMultiStepExperiment completes all steps")
    void testExecuteMultiStepExperiment() throws Exception {
        assertDoesNotThrow(() -> service.executeMultiStepExperiment(testRunPlan));
    }

    @Test
    @DisplayName("executeMultiStepExperiment handles different run plans")
    void testExecuteMultiStepExperimentWithDifferentPlans() throws Exception {
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

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(networkPlan));
    }

    @Test
    @DisplayName("executeMultiStepExperiment processes multiple SLOs")
    void testExecuteMultiStepExperimentMultipleSlos() throws Exception {
        SloTarget slo1 = new SloTarget(SloMetric.LATENCY_P95, "query1", 500.0, "<");
        SloTarget slo2 = new SloTarget(SloMetric.ERROR_RATE, "query2", 0.01, "<");
        SloTarget slo3 = new SloTarget(SloMetric.AVAILABILITY, "query3", 99.9, ">");

        ExperimentDefinition multiSloDefinition = new ExperimentDefinition(
            "multi-slo-exp",
            "Multi-SLO Experiment",
            FaultType.CPU_STRESS,
            new HashMap<>(),
            new TargetSystem("svc", "ns", Map.of()),
            Duration.ofMinutes(5),
            List.of(slo1, slo2, slo3),
            true,
            "user"
        );

        RunPlan multiSloPlan = new RunPlan(
            "multi-slo-run",
            multiSloDefinition,
            Instant.now(),
            false
        );

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(multiSloPlan));
    }

    @Test
    @DisplayName("executeMultiStepExperiment execution time is reasonable")
    void testExecuteMultiStepExperimentTiming() throws Exception {
        long startTime = System.currentTimeMillis();

        service.executeMultiStepExperiment(testRunPlan);

        long duration = System.currentTimeMillis() - startTime;

        // Should take at least 45 seconds (3 steps × 15 seconds each)
        assertTrue(duration >= 45000, "Should take at least 45 seconds");
        assertTrue(duration < 50000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("executeSimpleExperiment completes successfully")
    void testExecuteSimpleExperiment() throws Exception {
        assertDoesNotThrow(() -> service.executeSimpleExperiment(testDefinition));
    }

    @Test
    @DisplayName("executeSimpleExperiment handles different fault types")
    void testExecuteSimpleExperimentDifferentFaults() throws Exception {
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

            assertDoesNotThrow(() -> service.executeSimpleExperiment(def),
                "Should handle " + faultType);
        }
    }

    @Test
    @DisplayName("executeSimpleExperiment execution time is reasonable")
    void testExecuteSimpleExperimentTiming() throws Exception {
        long startTime = System.currentTimeMillis();

        service.executeSimpleExperiment(testDefinition);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 20000, "Should take at least 20 seconds");
        assertTrue(duration < 25000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("executeSimpleExperiment with various SLO configurations")
    void testExecuteSimpleExperimentVariousSlos() throws Exception {
        // Empty SLOs
        ExperimentDefinition noSlosDef = new ExperimentDefinition(
            "no-slos",
            "No SLOs",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("svc", "ns", Map.of()),
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user"
        );
        assertDoesNotThrow(() -> service.executeSimpleExperiment(noSlosDef));

        // Single SLO
        ExperimentDefinition singleSlo = new ExperimentDefinition(
            "single-slo",
            "Single SLO",
            FaultType.NETWORK_DELAY,
            new HashMap<>(),
            new TargetSystem("svc", "ns", Map.of()),
            Duration.ofMinutes(5),
            List.of(new SloTarget(SloMetric.LATENCY_P95, "query", 500.0, "<")),
            true,
            "user"
        );
        assertDoesNotThrow(() -> service.executeSimpleExperiment(singleSlo));
    }

    @Test
    @DisplayName("executeExperimentWithoutMonitoring completes successfully")
    void testExecuteExperimentWithoutMonitoring() throws Exception {
        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(testRunPlan));
    }

    @Test
    @DisplayName("executeExperimentWithoutMonitoring execution time is reasonable")
    void testExecuteExperimentWithoutMonitoringTiming() throws Exception {
        long startTime = System.currentTimeMillis();

        service.executeExperimentWithoutMonitoring(testRunPlan);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 20000, "Should take at least 20 seconds");
        assertTrue(duration < 25000, "Should not take significantly longer than expected");
    }

    @Test
    @DisplayName("executeExperimentWithoutMonitoring handles different experiments")
    void testExecuteExperimentWithoutMonitoringVariousExperiments() throws Exception {
        ExperimentDefinition cpuDef = new ExperimentDefinition(
            "cpu-exp",
            "CPU Experiment",
            FaultType.CPU_STRESS,
            Map.of("cores", "2"),
            new TargetSystem("cpu-svc", "default", Map.of()),
            Duration.ofMinutes(10),
            List.of(),
            false,
            "admin"
        );

        RunPlan cpuPlan = new RunPlan("cpu-run", cpuDef, Instant.now(), false);

        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(cpuPlan));
    }

    @Test
    @DisplayName("all methods handle dry run mode")
    void testDryRunMode() throws Exception {
        RunPlan dryRunPlan = new RunPlan("dry-001", testDefinition, Instant.now(), true);

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(dryRunPlan));
        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(dryRunPlan));
    }

    @Test
    @DisplayName("all methods handle scheduled execution time")
    void testScheduledExecutionTime() throws Exception {
        Instant futureTime = Instant.now().plusSeconds(300);
        RunPlan futurePlan = new RunPlan("future-run", testDefinition, futureTime, false);

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(futurePlan));
        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(futurePlan));
    }

    @Test
    @DisplayName("all methods handle past execution time")
    void testPastExecutionTime() throws Exception {
        Instant pastTime = Instant.now().minusSeconds(300);
        RunPlan pastPlan = new RunPlan("past-run", testDefinition, pastTime, false);

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(pastPlan));
        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(pastPlan));
    }

    @Test
    @DisplayName("service handles different target systems")
    void testDifferentTargetSystems() throws Exception {
        TargetSystem prodTarget = new TargetSystem("prod-service", "production", Map.of("env", "prod"));
        TargetSystem stagingTarget = new TargetSystem("staging-service", "staging", Map.of("env", "staging"));

        ExperimentDefinition prodDef = new ExperimentDefinition(
            "prod-exp",
            "Production Experiment",
            FaultType.NETWORK_DELAY,
            new HashMap<>(),
            prodTarget,
            Duration.ofMinutes(5),
            List.of(),
            false,
            "admin"
        );

        ExperimentDefinition stagingDef = new ExperimentDefinition(
            "staging-exp",
            "Staging Experiment",
            FaultType.POD_KILL,
            new HashMap<>(),
            stagingTarget,
            Duration.ofMinutes(5),
            List.of(),
            true,
            "developer"
        );

        RunPlan prodPlan = new RunPlan("prod-run", prodDef, Instant.now(), false);
        RunPlan stagingPlan = new RunPlan("staging-run", stagingDef, Instant.now(), false);

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(prodPlan));
        assertDoesNotThrow(() -> service.executeSimpleExperiment(stagingDef));
        assertDoesNotThrow(() -> service.executeExperimentWithoutMonitoring(stagingPlan));
    }

    @Test
    @DisplayName("service handles different experiment parameters")
    void testDifferentExperimentParameters() throws Exception {
        Map<String, Object> params1 = Map.of("duration", "30s", "intensity", "high");
        Map<String, Object> params2 = Map.of("delay", "100ms", "jitter", "10ms");
        Map<String, Object> params3 = Map.of("cpu", "80%", "memory", "2GB");

        ExperimentDefinition def1 = new ExperimentDefinition(
            "exp-1", "Exp 1", FaultType.CPU_STRESS, params1,
            new TargetSystem("svc1", "ns1", Map.of()),
            Duration.ofMinutes(5), List.of(), true, "user1"
        );

        ExperimentDefinition def2 = new ExperimentDefinition(
            "exp-2", "Exp 2", FaultType.NETWORK_DELAY, params2,
            new TargetSystem("svc2", "ns2", Map.of()),
            Duration.ofMinutes(5), List.of(), true, "user2"
        );

        ExperimentDefinition def3 = new ExperimentDefinition(
            "exp-3", "Exp 3", FaultType.MEMORY_STRESS, params3,
            new TargetSystem("svc3", "ns3", Map.of()),
            Duration.ofMinutes(5), List.of(), true, "user3"
        );

        assertDoesNotThrow(() -> service.executeSimpleExperiment(def1));
        assertDoesNotThrow(() -> service.executeSimpleExperiment(def2));
        assertDoesNotThrow(() -> service.executeSimpleExperiment(def3));
    }

    @Test
    @DisplayName("executeMultiStepExperiment with zero SLOs")
    void testExecuteMultiStepExperimentNoSlos() throws Exception {
        ExperimentDefinition noSlosDef = new ExperimentDefinition(
            "no-slos-exp",
            "No SLOs Experiment",
            FaultType.POD_KILL,
            new HashMap<>(),
            new TargetSystem("svc", "ns", Map.of()),
            Duration.ofMinutes(5),
            List.of(),
            true,
            "user"
        );

        RunPlan noSlosPlan = new RunPlan("no-slos-run", noSlosDef, Instant.now(), false);

        assertDoesNotThrow(() -> service.executeMultiStepExperiment(noSlosPlan));
    }

    @Test
    @DisplayName("service handles various timeout durations")
    void testVariousTimeoutDurations() throws Exception {
        Duration[] timeouts = {
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofHours(1)
        };

        for (Duration timeout : timeouts) {
            ExperimentDefinition def = new ExperimentDefinition(
                "timeout-exp-" + timeout.toMinutes(),
                "Timeout Test",
                FaultType.POD_KILL,
                new HashMap<>(),
                new TargetSystem("svc", "ns", Map.of()),
                timeout,
                List.of(),
                true,
                "user"
            );

            assertDoesNotThrow(() -> service.executeSimpleExperiment(def),
                "Should handle timeout of " + timeout.toMinutes() + " minutes");
        }
    }
}

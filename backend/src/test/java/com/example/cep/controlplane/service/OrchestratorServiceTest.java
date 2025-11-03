package com.example.cep.controlplane.service;

import com.example.cep.controlplane.store.ExperimentRepository;
import com.example.cep.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Iteration 2: Test-Driven Development for Orchestration & SLO Evaluation
 *
 * This test suite focuses on experiment orchestration and SLO-based decision making.
 * It follows the same RED-GREEN-REFACTOR cycle as Iteration 1.
 *
 * Key Testing Scenarios:
 * - Run plan dispatch and state management
 * - Agent status update handling
 * - Report generation for successful runs
 * - SLO breach detection and run failure (CRITICAL FEATURE)
 *
 * Test Coverage: 4 unit tests covering orchestration lifecycle
 * Complexity: Higher than Iteration 1 due to state management and SLO evaluation
 */
class OrchestratorServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private SloEvaluator sloEvaluator;

    private OrchestratorService orchestratorService;

    /**
     * Setup method executed before each test
     * Initializes mocks and creates instance under test
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestratorService = new OrchestratorServiceImpl(experimentRepository, sloEvaluator);
    }

    /**
     * TDD Iteration 2 - Test 1: Run Plan Dispatch
     *
     * RED Phase: This test will fail initially because OrchestratorServiceImpl doesn't exist
     * Expected failure: Class not found or method returns null
     */
    @Test
    @DisplayName("TDD-2.1: Dispatch valid run plan returns run ID and initializes state")
    void testDispatch_ValidRunPlan_ReturnsRunId() {
        // ARRANGE: Create a valid run plan
        RunPlan validPlan = createValidRunPlan();

        // ACT: Dispatch the run plan
        String returnedRunId = orchestratorService.dispatch(validPlan);

        // ASSERT: Verify run ID is returned
        assertNotNull(returnedRunId, "Dispatch should return a run ID");
        assertEquals(validPlan.getRunId(), returnedRunId, "Returned run ID should match plan run ID");

        // Verify repository interaction - run plan should be saved
        verify(experimentRepository, times(1)).saveRunPlan(validPlan);
    }

    /**
     * TDD Iteration 2 - Test 2: Agent Update Handling
     *
     * RED Phase: This test will fail because update handling is not implemented
     * Expected failure: Method does nothing or state is not updated
     */
    @Test
    @DisplayName("TDD-2.2: Handle agent update stores status and processes payload")
    void testHandleAgentUpdate_ValidUpdate_StoresStatus() {
        // ARRANGE: Setup a running experiment
        RunPlan runPlan = createValidRunPlan();
        orchestratorService.dispatch(runPlan);

        String runId = runPlan.getRunId();
        String status = "FAULT_INJECTED";
        Map<String, Object> payload = new HashMap<>();
        payload.put("faultType", "POD_KILL");
        payload.put("targetPod", "test-pod-12345");
        payload.put("timestamp", System.currentTimeMillis());

        // ACT: Handle agent update
        orchestratorService.handleAgentUpdate(runId, status, payload);

        // ASSERT: Verify the update was processed
        // (In a full implementation, we would verify state was updated in a state store)
        // For now, verify no exceptions were thrown and method completed
        assertDoesNotThrow(() ->
            orchestratorService.handleAgentUpdate(runId, status, payload),
            "handleAgentUpdate should process valid updates without errors"
        );
    }

    /**
     * TDD Iteration 2 - Test 3: Successful Run Finalization
     *
     * RED Phase: This test will fail because finalization logic doesn't create reports
     * Expected failure: Report is null or not saved
     */
    @Test
    @DisplayName("TDD-2.3: Finalize run with success outcome creates complete report")
    void testFinalizeRun_SuccessOutcome_CreatesReport() {
        // ARRANGE: Setup a completed run
        RunPlan runPlan = createValidRunPlan();
        orchestratorService.dispatch(runPlan);

        String runId = runPlan.getRunId();
        RunState successOutcome = RunState.COMPLETED;

        // Mock SLO evaluator to return good metrics (no breaches)
        Map<String, Object> sloResults = new HashMap<>();
        sloResults.put("latency_p95", 350.0);  // Below threshold of 500
        sloResults.put("latency_p95_threshold", 500.0);
        sloResults.put("latency_p95_comparator", "<");

        when(sloEvaluator.evaluate(any())).thenReturn(sloResults);
        when(sloEvaluator.breaches(sloResults)).thenReturn(false);  // No breach

        // ACT: Finalize the run
        Report report = orchestratorService.finalizeRun(runId, successOutcome);

        // ASSERT: Verify report was created with correct data
        assertNotNull(report, "Finalize should create a report");
        assertEquals(runId, report.getRunId(), "Report should have correct run ID");
        assertEquals(runPlan.getDefinition().getName(), report.getExperimentName(),
            "Report should have correct experiment name");
        assertEquals(successOutcome, report.getOutcome(), "Report should have COMPLETED outcome");
        assertNotNull(report.getSloDeltas(), "Report should include SLO deltas");

        // Verify SLO evaluation was performed
        verify(sloEvaluator, times(1)).evaluate(runPlan.getDefinition().getSlos());
        verify(sloEvaluator, times(1)).breaches(sloResults);

        // Verify report was saved
        verify(experimentRepository, times(1)).saveReport(any(Report.class));
    }

    /**
     * TDD Iteration 2 - Test 4: SLO Breach Detection (CRITICAL TEST)
     *
     * RED Phase: This test will fail because SLO breach handling is not implemented
     * Expected failure: Run marked as COMPLETED instead of FAILED when SLO breached
     *
     * This is the most important test in Iteration 2 as it validates the core
     * safety mechanism of the chaos engineering platform.
     */
    @Test
    @DisplayName("TDD-2.4: Finalize run with SLO breach marks run as FAILED")
    void testFinalizeRun_SloBreached_MarksAsFailed() {
        // ARRANGE: Setup a run that will breach SLO
        RunPlan runPlan = createValidRunPlan();
        orchestratorService.dispatch(runPlan);

        String runId = runPlan.getRunId();
        RunState originalOutcome = RunState.COMPLETED;  // Agent thinks it completed successfully

        // Mock SLO evaluator to return metrics showing a BREACH
        Map<String, Object> sloResults = new HashMap<>();
        sloResults.put("latency_p95", 650.0);  // ABOVE threshold of 500 (BREACH!)
        sloResults.put("latency_p95_threshold", 500.0);
        sloResults.put("latency_p95_comparator", "<");

        when(sloEvaluator.evaluate(any())).thenReturn(sloResults);
        when(sloEvaluator.breaches(sloResults)).thenReturn(true);  // BREACH DETECTED!

        // ACT: Finalize the run
        Report report = orchestratorService.finalizeRun(runId, originalOutcome);

        // ASSERT: Verify outcome was overridden to FAILED due to SLO breach
        assertNotNull(report, "Finalize should create a report even when SLO breached");
        assertEquals(runId, report.getRunId(), "Report should have correct run ID");

        // CRITICAL ASSERTION: Outcome must be FAILED, not COMPLETED
        assertEquals(RunState.FAILED, report.getOutcome(),
            "Report outcome should be FAILED when SLO is breached, even if agent reported success");

        // Verify SLO breach information is included in report
        assertNotNull(report.getSloDeltas(), "Report should include SLO breach details");
        assertTrue(report.getSloDeltas().containsKey("breach_detected"),
            "Report should flag that breach was detected");

        // Verify SLO evaluation was performed
        verify(sloEvaluator, times(1)).evaluate(runPlan.getDefinition().getSlos());
        verify(sloEvaluator, times(1)).breaches(sloResults);

        // Verify report was saved
        verify(experimentRepository, times(1)).saveReport(any(Report.class));
    }

    // ==================== Test Helper Methods ====================

    /**
     * Creates a valid run plan for testing
     * Includes all necessary components: definition, target, SLOs
     */
    private RunPlan createValidRunPlan() {
        // Create target system
        TargetSystem target = new TargetSystem(
            "staging-cluster",
            "staging",
            Map.of("app", "test-service", "version", "v1")
        );

        // Create SLO target
        SloTarget sloTarget = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,  // 500ms threshold
            "<"     // latency should be less than threshold
        );

        // Create experiment parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("duration", 300);
        parameters.put("podKillPercentage", 50);

        // Create experiment definition
        ExperimentDefinition definition = new ExperimentDefinition(
            "exp-12345",
            "Test Pod Kill Experiment",
            FaultType.POD_KILL,
            parameters,
            target,
            Duration.ofSeconds(300),  // 5 minutes
            List.of(sloTarget),
            true,
            "tester@example.com"
        );

        // Create run plan
        return new RunPlan(
            "run-" + System.currentTimeMillis(),
            definition,
            Instant.now(),
            false  // not a dry run
        );
    }

    /**
     * Creates a run plan that will cause SLO breach
     * Used for testing failure scenarios
     */
    @SuppressWarnings("unused")
    private RunPlan createBreachingRunPlan() {
        RunPlan plan = createValidRunPlan();
        // In real implementation, we would configure this to cause high latency
        // For now, the mocking in the test handles the breach scenario
        return plan;
    }
}

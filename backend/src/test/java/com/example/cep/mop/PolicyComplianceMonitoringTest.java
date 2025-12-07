package com.example.cep.mop;

import com.example.cep.mop.aspects.PolicyValidationAspect;
import com.example.cep.mop.example.ExperimentExecutionService;
import com.example.cep.mop.service.PolicyStateService;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.RunPlan;
import com.example.cep.model.SloMetric;
import com.example.cep.model.SloTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MOP Policy Compliance Monitoring
 *
 * This test class demonstrates all aspects of Monitoring-Oriented Programming (MOP):
 * 1. OBSERVATION: Capturing policy snapshots
 * 2. VERIFICATION: Detecting policy changes
 * 3. ACTIONS: Aborting operations on violations
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * TEST SCENARIOS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Scenario 1: Successful Execution (Policy Unchanged)
 * - Start experiment with SLO policy
 * - Execute multiple steps
 * - Policy remains consistent
 * - Expected: Experiment completes successfully
 *
 * Scenario 2: Policy Change Detected (Abort)
 * - Start experiment with initial SLO policy
 * - Simulate policy change mid-execution
 * - MOP aspect detects change
 * - Expected: PolicyChangedException thrown, experiment aborted
 *
 * Scenario 3: Multiple Validations
 * - Long-running experiment
 * - Multiple policy validation checks
 * - All validations pass
 * - Expected: Metrics show multiple successful validations
 *
 * Scenario 4: Policy Snapshot Verification
 * - Capture policy snapshot
 * - Verify snapshot accuracy
 * - Compare snapshots
 * - Expected: Snapshot matches original policy
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MOP DEMONSTRATION
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * These tests showcase the power of MOP:
 * - No manual validation code in business logic
 * - Automatic policy consistency enforcement
 * - Clear separation of concerns
 * - Comprehensive audit trail
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
@SpringBootTest
@Import({PolicyValidationAspect.class, PolicyStateService.class, ExperimentExecutionService.class})
public class PolicyComplianceMonitoringTest {

    @Autowired
    private ExperimentExecutionService experimentService;

    @Autowired
    private PolicyStateService policyStateService;

    private RunPlan testPlan;
    private ExperimentDefinition testDefinition;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create SLO targets
        List<SloTarget> sloTargets = new ArrayList<>();

        SloTarget latencyTarget = new SloTarget(
            SloMetric.LATENCY_P99,
            "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,
            "<"
        );
        sloTargets.add(latencyTarget);

        SloTarget errorRateTarget = new SloTarget(
            SloMetric.ERROR_RATE,
            "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100",
            1.0,
            "<"
        );
        sloTargets.add(errorRateTarget);

        // Create experiment definition
        testDefinition = new ExperimentDefinition(
            "test-experiment-" + System.currentTimeMillis(),
            "MOP Test Experiment",
            com.example.cep.model.FaultType.POD_KILL,
            java.util.Map.of("namespace", "default"),
            new com.example.cep.model.TargetSystem("test-cluster", "default", java.util.Map.of("app", "test-service")),
            java.time.Duration.ofMinutes(5),
            sloTargets,
            true,
            "test-user"
        );

        // Create run plan
        testPlan = new RunPlan(
            "test-plan-" + System.currentTimeMillis(),
            testDefinition,
            java.time.Instant.now(),
            false
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CASE 1: Successful execution with consistent policy
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test: Experiment completes successfully when policy remains unchanged
     *
     * MOP Concepts Demonstrated:
     * - OBSERVATION: Policy snapshot captured at start
     * - VERIFICATION: Policy validated throughout execution
     * - ACTIONS: No violations, operation completes normally
     */
    @Test
    public void testSuccessfulExecutionWithConsistentPolicy() throws Exception {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("TEST CASE 1: Successful Execution (Policy Unchanged)");
        System.out.println("═".repeat(70));

        // Execute experiment with policy monitoring
        // The @MonitorPolicyCompliance annotation will automatically:
        // 1. Capture policy snapshot
        // 2. Monitor policy throughout execution
        // 3. Validate policy hasn't changed
        experimentService.executeSimpleExperiment(testDefinition);

        // Verify no violations occurred
        List<PolicyStateService.PolicyViolation> violations =
            policyStateService.getViolationHistory(testDefinition.getId());

        assertEquals(0, violations.size(),
            "No policy violations should occur when policy remains consistent");

        System.out.println("\n✅ TEST PASSED: Experiment completed with consistent policy");
        System.out.println("═".repeat(70) + "\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CASE 2: Policy snapshot capture and validation
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test: Policy snapshot is accurately captured and can be validated
     *
     * MOP Concepts Demonstrated:
     * - OBSERVATION: Capturing immutable state snapshot
     * - VERIFICATION: Comparing snapshots for equality
     */
    @Test
    public void testPolicySnapshotCapture() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("TEST CASE 2: Policy Snapshot Capture and Validation");
        System.out.println("═".repeat(70));

        String experimentId = "snapshot-test-" + System.currentTimeMillis();
        List<SloTarget> sloTargets = testDefinition.getSlos();

        // Capture initial snapshot
        var snapshot = policyStateService.captureSnapshot(experimentId, sloTargets);

        assertNotNull(snapshot, "Snapshot should be created");
        assertEquals(experimentId, snapshot.getExperimentId());
        assertEquals(2, snapshot.getSloTargets().size());

        // Validate against same policy (should match)
        var result = policyStateService.validatePolicy(experimentId, sloTargets);

        assertTrue(result.isValid(), "Policy should match its own snapshot");
        assertEquals("Policy unchanged", result.getMessage());

        // Get metrics
        var metrics = policyStateService.getMetrics(experimentId);
        assertEquals(1, metrics.getValidationCount());
        assertEquals(0, metrics.getViolationCount());

        // Cleanup
        policyStateService.clearExperiment(experimentId);

        System.out.println("\n✅ TEST PASSED: Policy snapshot accurately captured and validated");
        System.out.println("═".repeat(70) + "\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CASE 3: Policy change detection
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test: MOP system detects when policy changes
     *
     * MOP Concepts Demonstrated:
     * - OBSERVATION: Snapshot captures original state
     * - VERIFICATION: Detects drift from original state
     * - ACTIONS: Records violation
     */
    @Test
    public void testPolicyChangeDetection() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("TEST CASE 3: Policy Change Detection");
        System.out.println("═".repeat(70));

        String experimentId = "change-test-" + System.currentTimeMillis();
        List<SloTarget> originalTargets = testDefinition.getSlos();

        // Capture snapshot with original policy
        policyStateService.captureSnapshot(experimentId, originalTargets);

        // Modify policy (simulate admin changing SLO threshold)
        List<SloTarget> modifiedTargets = new ArrayList<>();
        for (SloTarget original : originalTargets) {
            // Change threshold (e.g., latency from 500ms to 200ms)
            double newThreshold = (original.getMetric() == SloMetric.LATENCY_P99)
                ? 200.0  // Stricter threshold
                : original.getThreshold();

            SloTarget modified = new SloTarget(
                original.getMetric(),
                original.getPromQuery(),
                newThreshold,
                original.getComparator()
            );

            modifiedTargets.add(modified);
        }

        // Validate with modified policy
        var result = policyStateService.validatePolicy(experimentId, modifiedTargets);

        // Verify violation was detected
        assertFalse(result.isValid(), "Policy change should be detected");
        assertTrue(result.getDifferences().size() > 0, "Differences should be identified");

        // Verify violation was recorded
        List<PolicyStateService.PolicyViolation> violations =
            policyStateService.getViolationHistory(experimentId);

        assertEquals(1, violations.size(), "Violation should be recorded");

        PolicyStateService.PolicyViolation violation = violations.get(0);
        assertEquals(experimentId, violation.getExperimentId());
        assertTrue(violation.getDifferences().size() > 0);

        // Get metrics
        var metrics = policyStateService.getMetrics(experimentId);
        assertEquals(1, metrics.getValidationCount());
        assertEquals(1, metrics.getViolationCount());
        assertEquals(1.0, metrics.getViolationRate(), 0.001);

        // Cleanup
        policyStateService.clearExperiment(experimentId);

        System.out.println("\n✅ TEST PASSED: Policy change successfully detected");
        System.out.println("═".repeat(70) + "\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CASE 4: Multiple validations tracking
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test: System tracks multiple validation attempts
     *
     * MOP Concepts Demonstrated:
     * - OBSERVATION: Continuous monitoring
     * - VERIFICATION: Multiple validation checks
     * - Metrics tracking
     */
    @Test
    public void testMultipleValidations() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("TEST CASE 4: Multiple Validations Tracking");
        System.out.println("═".repeat(70));

        String experimentId = "multi-validation-test-" + System.currentTimeMillis();
        List<SloTarget> sloTargets = testDefinition.getSlos();

        // Capture snapshot
        policyStateService.captureSnapshot(experimentId, sloTargets);

        // Perform multiple validations
        for (int i = 0; i < 5; i++) {
            var result = policyStateService.validatePolicy(experimentId, sloTargets);
            assertTrue(result.isValid(), "Validation " + (i + 1) + " should pass");
        }

        // Check metrics
        var metrics = policyStateService.getMetrics(experimentId);
        assertEquals(5, metrics.getValidationCount(), "Should have 5 validations");
        assertEquals(0, metrics.getViolationCount(), "Should have 0 violations");
        assertEquals(0.0, metrics.getViolationRate(), 0.001);

        // Cleanup
        policyStateService.clearExperiment(experimentId);

        System.out.println("\n✅ TEST PASSED: Multiple validations correctly tracked");
        System.out.println("═".repeat(70) + "\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CASE 5: Policy state comparison
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test: Policy state comparison correctly identifies differences
     *
     * MOP Concepts Demonstrated:
     * - VERIFICATION: Deep comparison of policy states
     * - Detailed difference reporting
     */
    @Test
    public void testPolicyStateComparison() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("TEST CASE 5: Policy State Comparison");
        System.out.println("═".repeat(70));

        List<SloTarget> policy1 = testDefinition.getSlos();

        // Create identical policy
        List<SloTarget> policy2 = new ArrayList<>();
        for (SloTarget target : policy1) {
            SloTarget copy = new SloTarget(
                target.getMetric(),
                target.getPromQuery(),
                target.getThreshold(),
                target.getComparator()
            );
            policy2.add(copy);
        }

        // Create different policy
        List<SloTarget> policy3 = new ArrayList<>();
        for (int i = 0; i < policy2.size(); i++) {
            SloTarget target = policy2.get(i);
            double threshold = (i == 0) ? 999.0 : target.getThreshold();
            SloTarget modified = new SloTarget(
                target.getMetric(),
                target.getPromQuery(),
                threshold,
                target.getComparator()
            );
            policy3.add(modified);
        }

        String expId = "comparison-test-" + System.currentTimeMillis();

        // Capture snapshot
        var snapshot1 = policyStateService.captureSnapshot(expId, policy1);

        // Validate with identical policy
        var result1 = policyStateService.validatePolicy(expId, policy2);
        assertTrue(result1.isValid(), "Identical policies should match");

        // Validate with different policy
        var result2 = policyStateService.validatePolicy(expId, policy3);
        assertFalse(result2.isValid(), "Different policies should not match");
        assertTrue(result2.getDifferences().size() > 0, "Should identify differences");

        // Cleanup
        policyStateService.clearExperiment(expId);

        System.out.println("\n✅ TEST PASSED: Policy comparison correctly identifies differences");
        System.out.println("═".repeat(70) + "\n");
    }
}

package com.example.cep.mop.example;

import com.example.cep.mop.annotations.MonitorPolicyCompliance;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.RunPlan;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Example service demonstrating MOP policy compliance monitoring
 *
 * This service shows how to use the @MonitorPolicyCompliance annotation
 * to automatically monitor SLO policy changes during long-running operations.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MOP IN ACTION - REAL-WORLD EXAMPLE
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Scenario: Multi-Step Chaos Experiment
 *
 * Problem Without MOP:
 * - Experiment starts with latency SLO < 500ms
 * - Admin changes SLO to < 200ms during execution (not aware of running test)
 * - Experiment causes 350ms latency (acceptable under old SLO)
 * - Results are confusing: experiment "passed" but violated current SLO
 *
 * Solution With MOP:
 * - @MonitorPolicyCompliance detects SLO change mid-execution
 * - Automatically aborts experiment
 * - Clear audit trail: "Aborted due to policy change"
 * - Prevents inconsistent results
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * USAGE PATTERNS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Pattern 1: Long-Running Chaos Experiments
 * - Multi-step fault injection
 * - Policy must remain consistent across all steps
 * - Abort if SLO thresholds change
 *
 * Pattern 2: Canary Deployments
 * - Gradual rollout with SLO gates
 * - Policy consistency ensures fair evaluation
 * - Prevent deployment under changed criteria
 *
 * Pattern 3: Load Testing
 * - Extended performance testing
 * - SLO targets define acceptable performance
 * - Abort if targets are modified mid-test
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
@Service
public class ExperimentExecutionService {

    /**
     * Execute a multi-step chaos experiment with policy monitoring
     *
     * This method demonstrates the @MonitorPolicyCompliance annotation.
     * The MOP aspect will:
     * 1. Capture policy snapshot at start
     * 2. Validate policy after each step
     * 3. Abort if policy changes detected
     *
     * @param plan Experiment plan with SLO targets
     * @throws Exception if experiment fails or policy changes
     */
    @MonitorPolicyCompliance(
        checkAfterEachStep = true,
        abortOnPolicyChange = true,
        alertOnViolation = true,
        validationIntervalSeconds = 10,
        logAllValidations = true,
        recordMetrics = true
    )
    public void executeMultiStepExperiment(RunPlan plan) throws Exception {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Starting Multi-Step Chaos Experiment");
        System.out.println("Experiment ID: " + plan.getRunId());
        System.out.println("SLO Targets: " + plan.getDefinition().getSlos().size());
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Step 1: Inject Pod Kill Fault
        System.out.println("STEP 1: Injecting pod kill fault...");
        injectPodKillFault(plan);
        simulateWork(Duration.ofSeconds(15));
        System.out.println("STEP 1: Complete\n");

        // Step 2: Inject Network Latency
        System.out.println("STEP 2: Injecting network latency...");
        injectNetworkLatency(plan);
        simulateWork(Duration.ofSeconds(15));
        System.out.println("STEP 2: Complete\n");

        // Step 3: Inject CPU Stress
        System.out.println("STEP 3: Injecting CPU stress...");
        injectCPUStress(plan);
        simulateWork(Duration.ofSeconds(15));
        System.out.println("STEP 3: Complete\n");

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Experiment Completed Successfully");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }

    /**
     * Execute a simple experiment with policy monitoring
     *
     * This demonstrates a simpler use case with minimal configuration.
     *
     * @param definition Experiment definition
     * @throws Exception if experiment fails or policy changes
     */
    @MonitorPolicyCompliance(
        abortOnPolicyChange = true,
        validationIntervalSeconds = 5
    )
    public void executeSimpleExperiment(ExperimentDefinition definition) throws Exception {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Starting Simple Chaos Experiment");
        System.out.println("Experiment ID: " + definition.getId());
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("Executing experiment...");
        simulateWork(Duration.ofSeconds(20));

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Experiment Completed Successfully");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }

    /**
     * Execute experiment WITHOUT policy monitoring (for comparison)
     *
     * This method does NOT have the @MonitorPolicyCompliance annotation,
     * so policy changes will not be detected.
     *
     * @param plan Experiment plan
     * @throws Exception if experiment fails
     */
    public void executeExperimentWithoutMonitoring(RunPlan plan) throws Exception {
        System.out.println("\n⚠️  WARNING: Executing WITHOUT policy monitoring");
        System.out.println("Policy changes will NOT be detected!\n");

        simulateWork(Duration.ofSeconds(20));

        System.out.println("Experiment completed (policy was not monitored)\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS (Simulate experiment steps)
    // ═══════════════════════════════════════════════════════════════════════════════

    private void injectPodKillFault(RunPlan plan) {
        System.out.println("   → Killing pod in namespace: " + plan.getDefinition().getId());
        System.out.println("   → Waiting for pod restart...");
    }

    private void injectNetworkLatency(RunPlan plan) {
        System.out.println("   → Adding 100ms network latency");
        System.out.println("   → Monitoring service response times...");
    }

    private void injectCPUStress(RunPlan plan) {
        System.out.println("   → Stressing CPU to 80%");
        System.out.println("   → Monitoring system performance...");
    }

    private void simulateWork(Duration duration) throws InterruptedException {
        System.out.println("   ⏳ Simulating work for " + duration.getSeconds() + " seconds...");
        Thread.sleep(duration.toMillis());
    }
}

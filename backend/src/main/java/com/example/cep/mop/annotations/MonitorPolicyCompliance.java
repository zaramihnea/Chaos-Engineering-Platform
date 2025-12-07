package com.example.cep.mop.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable MOP-based monitoring of SLO policy compliance
 *
 * This annotation implements Monitoring-Oriented Programming (MOP) for policy
 * validation during long-running operations. It ensures that SLO policies remain
 * consistent throughout operation execution and aborts if policy changes are detected.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MOP CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. OBSERVATION (Runtime Monitoring):
 *    - Captures policy snapshot at operation start
 *    - Continuously observes policy state after each operation step
 *    - Detects policy modifications in real-time
 *
 * 2. VERIFICATION (Property Checking):
 *    - Verifies: "Policy at time T equals policy at time T0"
 *    - Temporal property: Policy consistency over time
 *    - Safety property: No unexpected policy changes during execution
 *
 * 3. ACTIONS (Violation Handling):
 *    - On Validation (policy unchanged):
 *      * Log compliance check
 *      * Continue operation
 *      * Update metrics
 *
 *    - On Violation (policy changed):
 *      * Abort operation immediately
 *      * Log policy drift details
 *      * Send alerts to administrators
 *      * Record audit trail
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHY POLICY CONSISTENCY MATTERS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Problem Scenario WITHOUT monitoring:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ T0: Start chaos experiment                                              │
 * │     SLO Policy: latency_p99 < 500ms                                      │
 * │     Decision: Safe to inject fault                                       │
 * │                                                                          │
 * │ T1: Admin changes policy (not aware of running experiment)              │
 * │     New SLO Policy: latency_p99 < 200ms                                  │
 * │                                                                          │
 * │ T2: Experiment causes latency spike to 350ms                            │
 * │     Under original policy: ✓ Within limits (< 500ms)                    │
 * │     Under new policy: ✗ VIOLATION (> 200ms)                             │
 * │                                                                          │
 * │ T3: Experiment completes successfully                                   │
 * │     But actually violated the current policy!                           │
 * │     Inconsistent behavior, audit trail confusion                        │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * Solution WITH MOP monitoring:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ T0: Start chaos experiment                                              │
 * │     MOP: Capture policy snapshot (latency_p99 < 500ms)                  │
 * │                                                                          │
 * │ T1: Admin changes policy to latency_p99 < 200ms                         │
 * │     MOP: Detect policy change at next validation checkpoint             │
 * │     MOP: ABORT experiment immediately                                   │
 * │     MOP: Log: "Policy changed during execution - operation aborted"     │
 * │     MOP: Alert administrators about policy drift                        │
 * │                                                                          │
 * │ Result: Consistent behavior, clear audit trail                          │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * COMPARISON: MOP vs TRADITIONAL APPROACH
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Traditional Approach (Manual Checking):
 * <pre>
 * public void executeExperiment(ExperimentDefinition def) {
 *     List&lt;SloTarget&gt; initialPolicy = def.getSlos();
 *
 *     // Step 1
 *     performStep1();
 *     if (!policiesMatch(initialPolicy, def.getSlos())) {
 *         throw new PolicyChangedException();
 *     }
 *
 *     // Step 2
 *     performStep2();
 *     if (!policiesMatch(initialPolicy, def.getSlos())) {
 *         throw new PolicyChangedException();
 *     }
 *
 *     // ... repeat for every step ...
 * }
 * </pre>
 *
 * Problems:
 * - Developer must remember to check after every step
 * - Easy to forget checks
 * - Code duplication
 * - Mixes business logic with monitoring logic
 *
 * MOP Approach (This Annotation):
 * <pre>
 * @MonitorPolicyCompliance(
 *     checkAfterEachStep = true,
 *     abortOnPolicyChange = true,
 *     alertOnViolation = true
 * )
 * public void executeExperiment(ExperimentDefinition def) {
 *     performStep1();
 *     performStep2();
 *     performStep3();
 *     // MOP aspect automatically validates policy after each step
 * }
 * </pre>
 *
 * Benefits:
 * - Automatic policy validation
 * - No manual checks needed
 * - Separation of concerns
 * - Consistent enforcement
 * - Easy to audit
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * USAGE EXAMPLES
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Example 1: Chaos Experiment with Policy Monitoring
 * <pre>
 * @MonitorPolicyCompliance(
 *     checkAfterEachStep = true,
 *     abortOnPolicyChange = true,
 *     alertOnViolation = true,
 *     validationIntervalSeconds = 30
 * )
 * public void executeMultiStepExperiment(RunPlan plan) {
 *     injectPodKillFault(plan);
 *     Thread.sleep(60000);
 *     injectNetworkLatency(plan);
 *     Thread.sleep(60000);
 *     // If SLO policy changes during any sleep, operation aborts
 * }
 * </pre>
 *
 * Example 2: Long-Running Deployment with SLO Gates
 * <pre>
 * @MonitorPolicyCompliance(
 *     checkAfterEachStep = true,
 *     abortOnPolicyChange = true
 * )
 * public void performCanaryDeployment(DeploymentPlan plan) {
 *     deployToCanary();
 *     waitAndMonitor(Duration.ofMinutes(10));
 *     deployToProduction();
 *     // Aborts if SLO thresholds change during deployment
 * }
 * </pre>
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPolicyCompliance {

    /**
     * Whether to validate policy after each operation step
     *
     * When true, the aspect will check policy consistency after each
     * method call within the annotated method.
     *
     * @return true to check after each step (default: true)
     */
    boolean checkAfterEachStep() default true;

    /**
     * Whether to abort operation when policy change is detected
     *
     * When true, throws PolicyChangedException on policy drift.
     * When false, logs warning but continues execution.
     *
     * @return true to abort on policy change (default: true)
     */
    boolean abortOnPolicyChange() default true;

    /**
     * Whether to send alerts when policy changes are detected
     *
     * When true, triggers alerts (logs, notifications) on policy violations.
     *
     * @return true to send alerts (default: true)
     */
    boolean alertOnViolation() default true;

    /**
     * Interval in seconds between policy validation checks
     *
     * For long-running operations, specifies how often to validate policy
     * consistency (in addition to step-based validation).
     *
     * @return validation interval in seconds (default: 60)
     */
    int validationIntervalSeconds() default 60;

    /**
     * Whether to log all policy validation checks
     *
     * When true, creates comprehensive audit trail of all validation attempts.
     * When false, only logs violations.
     *
     * @return true to log all validations (default: false)
     */
    boolean logAllValidations() default false;

    /**
     * Custom message to include in policy change alerts
     *
     * @return custom message (default: empty for default message)
     */
    String violationMessage() default "";

    /**
     * Whether to record metrics about policy compliance
     *
     * When true, tracks metrics like validation frequency, policy change rate, etc.
     *
     * @return true to record metrics (default: true)
     */
    boolean recordMetrics() default true;

    /**
     * Unique identifier for this monitoring session (for logging)
     *
     * @return monitoring ID (default: empty for auto-generated)
     */
    String monitoringId() default "";
}

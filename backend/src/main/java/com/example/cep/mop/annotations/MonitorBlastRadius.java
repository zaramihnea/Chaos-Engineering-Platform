package com.example.cep.mop.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable MOP-based Blast Radius Monitoring for chaos experiments
 *
 * This annotation implements Monitoring-Oriented Programming (MOP) to ensure that
 * chaos experiments don't spread beyond their intended scope. It continuously monitors
 * affected resources and aborts if the "blast radius" exceeds safety thresholds.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHY BLAST RADIUS MONITORING MATTERS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Problem Scenario WITHOUT monitoring:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ T0: Inject pod-kill fault targeting 1 pod in "cart" service             │
 * │     Expected: Only 1 pod affected                                        │
 * │                                                                          │
 * │ T1: Bug in fault injection → kills ALL pods matching label "app=cart"   │
 * │     Result: 5 pods killed instead of 1                                   │
 * │                                                                          │
 * │ T2: Cart service completely down                                        │
 * │     Cascading failure → payment service can't reach cart                │
 * │                                                                          │
 * │ T3: Entire checkout flow broken in production                           │
 * │     💥 PRODUCTION OUTAGE                                                 │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * Solution WITH Blast Radius Monitoring:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ T0: Start pod-kill with monitoring (maxAffectedPods = 1)                │
 * │     MOP: Capture initial state (0 affected pods)                         │
 * │                                                                          │
 * │ T1: Fault injection kills multiple pods                                 │
 * │     MOP: Detect 5 affected pods > threshold (1)                         │
 * │     MOP: ABORT experiment immediately                                   │
 * │     MOP: Trigger automatic rollback                                     │
 * │                                                                          │
 * │ T2: Alert sent: "Blast radius exceeded - experiment aborted"            │
 * │     Pods recover automatically                                           │
 * │                                                                          │
 * │ Result: Disaster prevented, system stays healthy                        │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MOP CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. OBSERVATION (Runtime Monitoring):
 *    - Tracks number of affected pods in real-time
 *    - Monitors affected namespaces
 *    - Counts impacted services
 *    - Records resource state changes
 *
 * 2. VERIFICATION (Safety Checking):
 *    - Verifies: affected_pods <= maxAffectedPods
 *    - Verifies: affected_namespaces <= maxAffectedNamespaces
 *    - Verifies: affected_services <= maxAffectedServices
 *    - Safety property: Blast radius stays within bounds
 *
 * 3. ACTIONS (Violation Handling):
 *    - On Safe Operation:
 *      * Log monitoring checks
 *      * Continue experiment
 *      * Update metrics
 *
 *    - On Blast Radius Exceeded:
 *      * ABORT experiment immediately
 *      * Trigger automatic rollback
 *      * Send critical alerts
 *      * Record incident details
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * USAGE EXAMPLES
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Example 1: Pod Kill with Strict Limits
 * <pre>
 * @MonitorBlastRadius(
 *     maxAffectedPods = 1,
 *     maxAffectedNamespaces = 1,
 *     checkIntervalSeconds = 5,
 *     abortOnBreach = true
 * )
 * public void injectPodKillFault(RunPlan plan) {
 *     // Kill one pod - if more than 1 affected, abort
 *     killPod(plan.getTargetPod());
 * }
 * </pre>
 *
 * Example 2: Network Latency with Service Scope
 * <pre>
 * @MonitorBlastRadius(
 *     maxAffectedServices = 2,
 *     maxAffectedPods = 5,
 *     abortOnBreach = true
 * )
 * public void injectNetworkLatency(RunPlan plan) {
 *     // If latency spreads to more than 2 services, abort
 *     addNetworkDelay(plan);
 * }
 * </pre>
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorBlastRadius {

    /**
     * Maximum number of pods that can be affected
     *
     * If the number of affected pods exceeds this threshold,
     * the experiment is aborted (if abortOnBreach is true).
     *
     * @return max affected pods (default: 3)
     */
    int maxAffectedPods() default 3;

    /**
     * Maximum number of namespaces that can be affected
     *
     * Prevents faults from spreading across namespace boundaries.
     *
     * @return max affected namespaces (default: 1)
     */
    int maxAffectedNamespaces() default 1;

    /**
     * Maximum number of services that can be affected
     *
     * Prevents cascading failures across multiple services.
     *
     * @return max affected services (default: 1)
     */
    int maxAffectedServices() default 1;

    /**
     * Interval in seconds between blast radius checks
     *
     * How often to verify the blast radius during experiment execution.
     *
     * @return check interval in seconds (default: 10)
     */
    int checkIntervalSeconds() default 10;

    /**
     * Whether to abort experiment when blast radius is breached
     *
     * When true, throws BlastRadiusExceededException on breach.
     * When false, logs warning but continues.
     *
     * @return true to abort on breach (default: true)
     */
    boolean abortOnBreach() default true;

    /**
     * Whether to trigger automatic rollback on breach
     *
     * When true, attempts to restore affected resources to original state.
     *
     * @return true to auto-rollback (default: true)
     */
    boolean autoRollback() default true;

    /**
     * Whether to send alerts when blast radius is breached
     *
     * @return true to send alerts (default: true)
     */
    boolean alertOnBreach() default true;

    /**
     * Whether to log all blast radius checks
     *
     * When true, creates detailed audit trail of all checks.
     * When false, only logs breaches.
     *
     * @return true to log all checks (default: false)
     */
    boolean logAllChecks() default false;

    /**
     * Custom message to include in breach alerts
     *
     * @return custom message (default: empty for default message)
     */
    String breachMessage() default "";

    /**
     * Unique identifier for this monitoring session
     *
     * @return monitoring ID (default: empty for auto-generated)
     */
    String monitoringId() default "";
}

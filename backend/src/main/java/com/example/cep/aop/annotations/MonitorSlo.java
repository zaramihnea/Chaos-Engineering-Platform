package com.example.cep.aop.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable continuous SLO monitoring for long-running operations
 *
 * This annotation is specifically designed for methods that perform long-running
 * chaos experiments where real-time SLO monitoring is critical. Unlike @ValidateSlo,
 * which validates at specific lifecycle points, @MonitorSlo enables active monitoring
 * throughout the entire method execution.
 *
 * Key Features:
 * - Periodic SLO evaluation during method execution
 * - Automatic experiment abort when SLO breach detected
 * - Real-time alerting and logging
 * - Configurable monitoring interval and timeout
 *
 * AOP Implementation:
 * Uses AspectJ @Around advice to wrap the method execution and spawn a background
 * monitoring thread that periodically queries Prometheus and evaluates SLOs.
 *
 * Cross-Cutting Concern:
 * Continuous monitoring is a perfect example of a cross-cutting concern:
 * - Applies to multiple experiment execution methods
 * - Independent of business logic
 * - Requires consistent implementation across all chaos operations
 *
 * Without AOP:
 * - Each experiment method would need embedded monitoring code
 * - Difficult to ensure consistent monitoring behavior
 * - Code duplication and maintenance burden
 *
 * With AOP:
 * - Single aspect handles all monitoring
 * - Declarative monitoring via annotation
 * - Easy to add monitoring to new methods
 *
 * Usage Example:
 * <pre>
 * {@code
 * @MonitorSlo(intervalSeconds = 10, maxDurationSeconds = 300, abortOnBreach = true)
 * public void executeExperiment(RunPlan plan) {
 *     // Long-running chaos experiment
 *     injectPodKillFault(plan);
 *     Thread.sleep(Duration.ofMinutes(5));
 * }
 * }
 * </pre>
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorSlo {

    /**
     * Interval in seconds between SLO evaluation checks
     *
     * @return interval in seconds (default: 15)
     */
    int intervalSeconds() default 15;

    /**
     * Maximum duration in seconds to monitor before automatic timeout
     *
     * @return max duration in seconds (default: 600 = 10 minutes)
     */
    int maxDurationSeconds() default 600;

    /**
     * Whether to abort the operation if SLO breach is detected
     *
     * @return true to abort on breach (default: true)
     */
    boolean abortOnBreach() default true;

    /**
     * Whether to send alerts when SLO breach is detected
     *
     * @return true to send alerts (default: true)
     */
    boolean alertOnBreach() default true;

    /**
     * Custom identifier for this monitoring session (used in logs)
     *
     * @return monitoring session identifier (default: empty for auto-generated)
     */
    String monitoringId() default "";
}

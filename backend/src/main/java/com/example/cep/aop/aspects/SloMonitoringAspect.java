package com.example.cep.aop.aspects;

import com.example.cep.aop.annotations.MonitorSlo;
import com.example.cep.controlplane.service.SloEvaluator;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.RunPlan;
import com.example.cep.model.SloTarget;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOP Aspect for Continuous SLO Monitoring during Long-Running Experiments
 *
 * This aspect implements real-time SLO monitoring for chaos engineering experiments.
 * While SloValidationAspect validates SLOs at specific lifecycle points (before/after),
 * this aspect provides continuous monitoring throughout the entire experiment execution.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHY CONTINUOUS MONITORING IS NEEDED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Problem:
 * Chaos experiments can run for minutes or hours. Validating SLOs only before/after
 * means we could miss critical degradation that occurs during the experiment.
 *
 * Example Scenario:
 * 1. T+0s:  Experiment starts, baseline SLOs are good ✓
 * 2. T+30s: Inject pod kill fault
 * 3. T+45s: Latency spikes to 5000ms (⚠️ SLO breach - but not detected!)
 * 4. T+90s: System recovers, latency back to 200ms
 * 5. T+120s: Experiment ends, final SLOs are good ✓
 *
 * Without continuous monitoring, we would think the experiment passed, but we missed
 * a 45-second period of severe degradation!
 *
 * Solution:
 * This aspect spawns a background monitoring thread that:
 * - Queries Prometheus every N seconds
 * - Evaluates SLO thresholds in real-time
 * - Triggers immediate abort if breach detected
 * - Logs all violations for post-experiment analysis
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * AOP CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. CROSS-CUTTING CONCERN:
 *    - Monitoring applies to multiple experiment types (pod kill, CPU hog, network delay)
 *    - Without AOP, each experiment would need embedded monitoring code
 *    - With AOP, add @MonitorSlo annotation to any method
 *
 * 2. SEPARATION OF CONCERNS:
 *    - Business logic (experiment execution) separated from monitoring logic
 *    - Developers focus on experiment implementation
 *    - Aspect handles all monitoring complexity
 *
 * 3. @AROUND ADVICE:
 *    - Most powerful advice type - can control method execution flow
 *    - Starts monitoring thread before method
 *    - Waits for method completion
 *    - Stops monitoring thread after method
 *    - Can abort method if SLO breach detected
 *
 * 4. CONCURRENT PROGRAMMING:
 *    - Demonstrates AOP integration with Java concurrency
 *    - Uses ExecutorService for thread management
 *    - AtomicBoolean for thread-safe flags
 *    - Proper resource cleanup (shutdown executor)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * INTEGRATION WITH PROMETHEUS & CHAOS PLATFORM
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Architecture:
 * ┌──────────────┐       ┌─────────────────────┐       ┌────────────┐
 * │ Orchestrator │──────>│ SloMonitoringAspect │──────>│ Prometheus │
 * │   Service    │       │  (This Class)       │       │   Client   │
 * └──────────────┘       └─────────────────────┘       └────────────┘
 *        │                        │
 *        │                        ├─> Spawn monitoring thread
 *        │                        ├─> Query every 15s
 *        │                        ├─> Evaluate SLOs
 *        │                        └─> Abort if breach
 *        │
 *        └─> Execute chaos fault
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Aspect
@Component
public class SloMonitoringAspect {

    private final SloEvaluator sloEvaluator;
    private final ExecutorService monitoringExecutor;

    /**
     * Constructor with dependency injection
     *
     * @param sloEvaluator Service for evaluating SLO metrics
     */
    public SloMonitoringAspect(SloEvaluator sloEvaluator) {
        this.sloEvaluator = sloEvaluator;
        // Create thread pool for monitoring tasks
        this.monitoringExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("SLO-Monitor-" + System.currentTimeMillis());
            t.setDaemon(true); // Daemon threads don't prevent JVM shutdown
            return t;
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AROUND ADVICE - Wraps method with continuous monitoring
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Continuous SLO monitoring advice
     *
     * This advice wraps the target method and spawns a background thread that
     * continuously monitors SLOs throughout the method execution.
     *
     * Execution Flow:
     * 1. Extract SLO targets from method arguments
     * 2. Generate unique monitoring session ID
     * 3. Start background monitoring thread
     * 4. Execute the wrapped method
     * 5. Stop monitoring thread
     * 6. Return result or throw exception
     *
     * Monitoring Thread Behavior:
     * - Runs in background while method executes
     * - Queries Prometheus every N seconds (configurable)
     * - Evaluates SLO thresholds
     * - Sets abort flag if breach detected
     * - Continues until method completes or timeout reached
     *
     * @param joinPoint Join point with method context
     * @param monitorSlo Annotation instance with monitoring configuration
     * @return Method return value
     * @throws Throwable if method fails or SLO breach occurs
     */
    @Around("@annotation(monitorSlo)")
    public Object monitorSlosDuringExecution(ProceedingJoinPoint joinPoint, MonitorSlo monitorSlo) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        // Generate unique monitoring session ID
        String monitoringId = monitorSlo.monitoringId().isEmpty()
            ? UUID.randomUUID().toString().substring(0, 8)
            : monitorSlo.monitoringId();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ AOP ASPECT: Continuous SLO Monitoring                        ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Method:        " + String.format("%-45s", methodName) + " ║");
        System.out.println("║ Monitor ID:    " + String.format("%-45s", monitoringId) + " ║");
        System.out.println("║ Interval:      " + String.format("%-45s", monitorSlo.intervalSeconds() + "s") + " ║");
        System.out.println("║ Max Duration:  " + String.format("%-45s", monitorSlo.maxDurationSeconds() + "s") + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 1: Extract SLO targets from method arguments
        // ──────────────────────────────────────────────────────────────────────────
        List<SloTarget> sloTargets = extractSloTargets(joinPoint);

        if (sloTargets == null || sloTargets.isEmpty()) {
            System.out.println("⚠️  No SLO targets found - skipping continuous monitoring");
            return joinPoint.proceed();
        }

        System.out.println("📊 Monitoring " + sloTargets.size() + " SLO target(s)\n");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 2: Initialize monitoring state
        // ──────────────────────────────────────────────────────────────────────────
        AtomicBoolean shouldAbort = new AtomicBoolean(false);
        AtomicBoolean isRunning = new AtomicBoolean(true);
        AtomicInteger checkCount = new AtomicInteger(0);
        AtomicInteger breachCount = new AtomicInteger(0);
        ConcurrentHashMap<String, Object> lastBreachResults = new ConcurrentHashMap<>();

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 3: Start background monitoring thread
        // ──────────────────────────────────────────────────────────────────────────
        Future<?> monitoringFuture = monitoringExecutor.submit(() -> {
            monitoringLoop(
                monitoringId,
                sloTargets,
                monitorSlo,
                shouldAbort,
                isRunning,
                checkCount,
                breachCount,
                lastBreachResults
            );
        });

        System.out.println("🚀 Background monitoring thread started");
        System.out.println("⏳ Proceeding with method execution...\n");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 4: Execute the wrapped method
        // ──────────────────────────────────────────────────────────────────────────
        Object result;
        Throwable methodException = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            methodException = e;
            result = null;
        } finally {
            // ──────────────────────────────────────────────────────────────────────
            // STEP 5: Stop monitoring thread
            // ──────────────────────────────────────────────────────────────────────
            isRunning.set(false);
            monitoringFuture.cancel(true);

            System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║ Monitoring Session Completed                                 ║");
            System.out.println("╠════════════════════════════════════════════════════════════════╣");
            System.out.println("║ Monitor ID:    " + String.format("%-45s", monitoringId) + " ║");
            System.out.println("║ Total Checks:  " + String.format("%-45s", checkCount.get()) + " ║");
            System.out.println("║ Breaches:      " + String.format("%-45s", breachCount.get()) + " ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
        }

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 6: Check for abort conditions and return result
        // ──────────────────────────────────────────────────────────────────────────
        if (shouldAbort.get() && monitorSlo.abortOnBreach()) {
            System.out.println("❌ EXPERIMENT ABORTED due to SLO breach during execution\n");
            throw new SloBreachDuringExecutionException(
                "Continuous monitoring detected SLO breach - experiment aborted",
                lastBreachResults
            );
        }

        if (methodException != null) {
            throw methodException;
        }

        if (breachCount.get() > 0) {
            System.out.println("⚠️  Experiment completed but " + breachCount.get() + " SLO breach(es) occurred\n");
        } else {
            System.out.println("✅ Experiment completed with no SLO breaches\n");
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MONITORING LOOP - Background thread logic
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Main monitoring loop that runs in background thread
     *
     * This method continuously queries Prometheus and evaluates SLOs until:
     * - The wrapped method completes (isRunning=false)
     * - Maximum duration timeout is reached
     * - Thread is interrupted
     *
     * @param monitoringId Unique identifier for this monitoring session
     * @param sloTargets List of SLO targets to monitor
     * @param config Monitoring configuration from annotation
     * @param shouldAbort Flag to signal abort to main thread
     * @param isRunning Flag indicating if method is still executing
     * @param checkCount Counter for total SLO checks performed
     * @param breachCount Counter for total breaches detected
     * @param lastBreachResults Storage for most recent breach results
     */
    private void monitoringLoop(
        String monitoringId,
        List<SloTarget> sloTargets,
        MonitorSlo config,
        AtomicBoolean shouldAbort,
        AtomicBoolean isRunning,
        AtomicInteger checkCount,
        AtomicInteger breachCount,
        ConcurrentHashMap<String, Object> lastBreachResults
    ) {
        Instant startTime = Instant.now();
        Duration maxDuration = Duration.ofSeconds(config.maxDurationSeconds());
        Duration interval = Duration.ofSeconds(config.intervalSeconds());

        System.out.println("┌─ Monitoring Loop Started ────────────────────────────────────┐");
        System.out.println("│ Session:  " + monitoringId);
        System.out.println("│ Interval: " + config.intervalSeconds() + "s");
        System.out.println("│ Max Time: " + config.maxDurationSeconds() + "s");
        System.out.println("└───────────────────────────────────────────────────────────────┘\n");

        while (isRunning.get()) {
            try {
                // Check if max duration exceeded
                Duration elapsed = Duration.between(startTime, Instant.now());
                if (elapsed.compareTo(maxDuration) > 0) {
                    System.out.println("⏱️  Maximum monitoring duration reached - stopping");
                    break;
                }

                // Perform SLO check
                int currentCheck = checkCount.incrementAndGet();
                System.out.println("🔍 Check #" + currentCheck + " @ " + Instant.now());

                Map<String, Object> results = sloEvaluator.evaluate(sloTargets);
                boolean breach = sloEvaluator.breaches(results);

                if (breach) {
                    int totalBreaches = breachCount.incrementAndGet();
                    lastBreachResults.clear();
                    lastBreachResults.putAll(results);

                    System.out.println("   ❌ BREACH DETECTED (total: " + totalBreaches + ")");
                    logBreachDetails(results);

                    if (config.abortOnBreach()) {
                        System.out.println("   🛑 Setting abort flag");
                        shouldAbort.set(true);
                        if (config.alertOnBreach()) {
                            sendAlert(monitoringId, results);
                        }
                        break; // Exit monitoring loop
                    }
                } else {
                    System.out.println("   ✅ All SLOs within thresholds");
                }

                // Sleep until next check
                Thread.sleep(interval.toMillis());

            } catch (InterruptedException e) {
                System.out.println("⚠️  Monitoring thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("❌ Error during monitoring: " + e.getMessage());
                // Continue monitoring despite errors
            }
        }

        System.out.println("\n┌─ Monitoring Loop Stopped ────────────────────────────────────┐");
        System.out.println("│ Total Checks: " + checkCount.get());
        System.out.println("│ Breaches:     " + breachCount.get());
        System.out.println("└───────────────────────────────────────────────────────────────┘");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Extract SLO targets from method arguments
     */
    private List<SloTarget> extractSloTargets(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof RunPlan) {
                return ((RunPlan) arg).getDefinition().getSlos();
            } else if (arg instanceof ExperimentDefinition) {
                return ((ExperimentDefinition) arg).getSlos();
            }
        }
        return null;
    }

    /**
     * Log detailed breach information
     */
    private void logBreachDetails(Map<String, Object> results) {
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String key = entry.getKey();
            if (!key.contains("_threshold") && !key.contains("_comparator") && !key.contains("_error")) {
                Object value = entry.getValue();
                Object threshold = results.get(key + "_threshold");
                Object comparator = results.get(key + "_comparator");
                if (threshold != null) {
                    System.out.println("      " + key + ": " + value + " (threshold: " + comparator + " " + threshold + ")");
                }
            }
        }
    }

    /**
     * Send alert when SLO breach detected
     * (Placeholder for future integration with alerting systems)
     */
    private void sendAlert(String monitoringId, Map<String, Object> results) {
        System.out.println("   📢 ALERT: SLO breach in monitoring session " + monitoringId);
        // TODO: Integrate with PagerDuty, Slack, email, etc.
    }

    /**
     * Cleanup method called on bean destruction
     */
    public void destroy() {
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitoringExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // EXCEPTION CLASS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Exception thrown when SLO breach detected during continuous monitoring
     */
    public static class SloBreachDuringExecutionException extends RuntimeException {
        private final Map<String, Object> sloResults;

        public SloBreachDuringExecutionException(String message, Map<String, Object> sloResults) {
            super(message);
            this.sloResults = sloResults;
        }

        public Map<String, Object> getSloResults() {
            return sloResults;
        }
    }
}

package com.example.cep.mop.aspects;

import com.example.cep.mop.annotations.MonitorBlastRadius;
import com.example.cep.mop.model.BlastRadiusState;
import com.example.cep.mop.service.BlastRadiusService;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.RunPlan;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MOP Aspect for Blast Radius Monitoring
 *
 * This aspect implements Monitoring-Oriented Programming (MOP) for tracking and limiting
 * the blast radius of chaos experiments. It ensures that faults don't spread beyond
 * their intended scope by continuously monitoring affected resources.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MONITORING-ORIENTED PROGRAMMING (MOP) - BLAST RADIUS IMPLEMENTATION
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * This aspect demonstrates the three pillars of MOP:
 *
 * 1. OBSERVATION (Real-time Resource Tracking):
 *    - Monitors number of affected pods
 *    - Tracks affected namespaces
 *    - Counts impacted services
 *    - Discovers resources during experiment execution
 *
 * 2. VERIFICATION (Safety Boundary Checking):
 *    - Validates: affected_pods <= maxAffectedPods
 *    - Validates: affected_namespaces <= maxAffectedNamespaces
 *    - Validates: affected_services <= maxAffectedServices
 *    - Safety property: Blast radius stays within defined bounds
 *
 * 3. ACTIONS (Breach Response):
 *    On Safe Operation:
 *    - Log monitoring status
 *    - Continue experiment
 *    - Update metrics
 *
 *    On Blast Radius Breach:
 *    - ABORT experiment immediately
 *    - Trigger automatic rollback (if enabled)
 *    - Send critical alerts
 *    - Throw BlastRadiusExceededException
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * RUNTIME BEHAVIOR
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Execution Flow:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 1. Method call: injectPodKillFault(plan)                                   │
 * │    ↓                                                                        │
 * │ 2. Aspect intercepts (BEFORE method execution)                             │
 * │    - Initialize blast radius tracking                                      │
 * │    - Set safety thresholds (maxPods=1, maxNamespaces=1, etc.)             │
 * │    - Start background monitoring thread                                    │
 * │    ↓                                                                        │
 * │ 3. Proceed with fault injection                                            │
 * │    ↓                                                                        │
 * │ 4. Background thread monitors in parallel:                                 │
 * │    - Every N seconds: discover affected resources                          │
 * │    - Check blast radius against thresholds                                 │
 * │    - If exceeded: set abort flag                                           │
 * │    ↓                                                                        │
 * │ 5. Method completes (or is aborted)                                        │
 * │    ↓                                                                        │
 * │ 6. Aspect checks abort flag (AFTER method execution)                       │
 * │    - If breached: throw BlastRadiusExceededException                       │
 * │    - If safe: return result                                                │
 * │    ↓                                                                        │
 * │ 7. Cleanup: Stop monitoring, clear state                                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Aspect
@Component
public class BlastRadiusMonitoringAspect {

    private final BlastRadiusService blastRadiusService;
    private final ExecutorService monitoringExecutor;

    /**
     * Constructor with dependency injection
     *
     * @param blastRadiusService Service for blast radius tracking
     */
    public BlastRadiusMonitoringAspect(BlastRadiusService blastRadiusService) {
        this.blastRadiusService = blastRadiusService;
        this.monitoringExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("MOP-BlastRadiusMonitor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AROUND ADVICE - Blast Radius Monitoring
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Main MOP monitoring advice
     *
     * This advice wraps the target method and implements complete MOP monitoring:
     * observation, verification, and action triggering.
     *
     * @param joinPoint Join point with method context
     * @param monitor Annotation instance with configuration
     * @return Method return value
     * @throws Throwable if method fails or blast radius breach occurs
     */
    @Around("@annotation(monitor)")
    public Object monitorBlastRadius(
        ProceedingJoinPoint joinPoint,
        MonitorBlastRadius monitor
    ) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        // Generate monitoring ID
        String monitoringId = monitor.monitoringId().isEmpty()
            ? UUID.randomUUID().toString().substring(0, 8)
            : monitor.monitoringId();

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ MOP ASPECT: Blast Radius Monitoring                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Method:      " + String.format("%-47s", methodName) + " ║");
        System.out.println("║ Monitor ID:  " + String.format("%-47s", monitoringId) + " ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Safety Thresholds:                                           ║");
        System.out.println("║   Max Pods:       " + String.format("%-42d", monitor.maxAffectedPods()) + " ║");
        System.out.println("║   Max Namespaces: " + String.format("%-42d", monitor.maxAffectedNamespaces()) + " ║");
        System.out.println("║   Max Services:   " + String.format("%-42d", monitor.maxAffectedServices()) + " ║");
        System.out.println("║   Check Interval: " + String.format("%-42s", monitor.checkIntervalSeconds() + "s") + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 1: OBSERVATION - Initialize blast radius tracking
        // ──────────────────────────────────────────────────────────────────────────
        String experimentId = extractExperimentId(joinPoint);
        if (experimentId == null) {
            experimentId = "experiment-" + monitoringId;
        }

        // Make experimentId final for lambda
        final String finalExperimentId = experimentId;

        System.out.println("🎯 Initializing blast radius tracking for: " + experimentId);
        blastRadiusService.initializeTracking(experimentId);

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 2: Start background monitoring thread
        // ──────────────────────────────────────────────────────────────────────────
        AtomicBoolean shouldAbort = new AtomicBoolean(false);
        AtomicBoolean isRunning = new AtomicBoolean(true);
        AtomicInteger checkCount = new AtomicInteger(0);

        Future<?> monitoringFuture = monitoringExecutor.submit(() -> {
            monitoringLoop(
                finalExperimentId,
                monitoringId,
                monitor,
                shouldAbort,
                isRunning,
                checkCount
            );
        });

        System.out.println("🚀 Background blast radius monitoring started");
        System.out.println("⏳ Proceeding with method execution...\n");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 3: Execute the wrapped method
        // ──────────────────────────────────────────────────────────────────────────
        Object result;
        Throwable methodException = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            methodException = e;
            result = null;
        } finally {
            // Stop monitoring
            isRunning.set(false);
            monitoringFuture.cancel(true);

            // Get final blast radius state
            BlastRadiusState finalState = blastRadiusService.getCurrentState(finalExperimentId);

            System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║ MOP Monitoring Session Completed                             ║");
            System.out.println("╠════════════════════════════════════════════════════════════════╣");
            System.out.println("║ Monitor ID:       " + String.format("%-44s", monitoringId) + " ║");
            System.out.println("║ Checks Performed: " + String.format("%-44d", checkCount.get()) + " ║");
            System.out.println("║ Blast Radius:                                                ║");
            if (finalState != null) {
                System.out.println("║   Affected Pods:       " + String.format("%-35d", finalState.getAffectedPodCount()) + " ║");
                System.out.println("║   Affected Namespaces: " + String.format("%-35d", finalState.getAffectedNamespaceCount()) + " ║");
                System.out.println("║   Affected Services:   " + String.format("%-35d", finalState.getAffectedServiceCount()) + " ║");
            }
            System.out.println("║ Breach Detected:  " + String.format("%-44s", shouldAbort.get() ? "YES ⚠️" : "NO ✅") + " ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        }

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 4: ACTIONS - Handle validation or breach
        // ──────────────────────────────────────────────────────────────────────────

        // Check if blast radius breach detected
        if (shouldAbort.get() && monitor.abortOnBreach()) {
            System.out.println("❌ OPERATION ABORTED: Blast radius exceeded safety thresholds\n");

            // Get breach details
            List<BlastRadiusService.BlastRadiusBreach> breaches =
                blastRadiusService.getBreachHistory(finalExperimentId);

            String message = monitor.breachMessage().isEmpty()
                ? "Blast radius exceeded safety thresholds - experiment aborted"
                : monitor.breachMessage();

            // Trigger rollback if enabled
            if (monitor.autoRollback()) {
                System.out.println("🔄 Triggering automatic rollback...");
                // TODO: Implement actual rollback logic
            }

            blastRadiusService.clearExperiment(finalExperimentId);

            throw new BlastRadiusExceededException(message, finalExperimentId, breaches);
        }

        // Clean up blast radius state
        blastRadiusService.clearExperiment(finalExperimentId);

        // Rethrow any exception from method
        if (methodException != null) {
            throw methodException;
        }

        System.out.println("✅ Operation completed within safe blast radius limits\n");
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MONITORING LOOP - Background Verification
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Background monitoring loop that continuously checks blast radius
     *
     * @param experimentId Experiment identifier
     * @param monitoringId Monitoring session ID
     * @param config Monitoring configuration
     * @param shouldAbort Flag to signal abort
     * @param isRunning Flag indicating if method is executing
     * @param checkCount Counter for checks performed
     */
    private void monitoringLoop(
        String experimentId,
        String monitoringId,
        MonitorBlastRadius config,
        AtomicBoolean shouldAbort,
        AtomicBoolean isRunning,
        AtomicInteger checkCount
    ) {
        Duration interval = Duration.ofSeconds(config.checkIntervalSeconds());

        System.out.println("┌─ MOP Blast Radius Monitoring Loop Started ───────────────────┐");
        System.out.println("│ Experiment:  " + experimentId);
        System.out.println("│ Interval:    " + config.checkIntervalSeconds() + "s");
        System.out.println("└───────────────────────────────────────────────────────────────┘\n");

        while (isRunning.get()) {
            try {
                Thread.sleep(interval.toMillis());

                // ──────────────────────────────────────────────────────────────────
                // OBSERVATION: Discover affected resources
                // ──────────────────────────────────────────────────────────────────
                int currentCheck = checkCount.incrementAndGet();

                if (config.logAllChecks()) {
                    System.out.println("🔍 Blast Radius Check #" + currentCheck);
                }

                // In real implementation, this would query Kubernetes API
                // For demo, we simulate resource discovery
                // Note: Pass false for normal operation, true to simulate breach
                blastRadiusService.simulateResourceDiscovery(experimentId, false);

                // ──────────────────────────────────────────────────────────────────
                // VERIFICATION: Check if blast radius exceeds thresholds
                // ──────────────────────────────────────────────────────────────────
                BlastRadiusService.ValidationResult result =
                    blastRadiusService.validateBlastRadius(
                        experimentId,
                        config.maxAffectedPods(),
                        config.maxAffectedNamespaces(),
                        config.maxAffectedServices()
                    );

                if (!result.isValid()) {
                    // ──────────────────────────────────────────────────────────────
                    // BREACH DETECTED
                    // ──────────────────────────────────────────────────────────────
                    System.out.println("   ❌ BLAST RADIUS BREACH DETECTED");
                    System.out.println("   Violations:");
                    result.getBreaches().forEach(breach ->
                        System.out.println("     - " + breach)
                    );

                    if (config.alertOnBreach()) {
                        sendAlert(monitoringId, experimentId, result);
                    }

                    if (config.abortOnBreach()) {
                        System.out.println("   🛑 Setting abort flag");
                        shouldAbort.set(true);
                        break; // Exit monitoring loop
                    }
                } else {
                    if (config.logAllChecks()) {
                        System.out.println("   ✅ Blast radius within limits");
                    }
                }

            } catch (InterruptedException e) {
                System.out.println("⚠️  Monitoring thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("❌ Error during blast radius check: " + e.getMessage());
            }
        }

        System.out.println("\n┌─ MOP Blast Radius Monitoring Loop Stopped ───────────────────┐");
        System.out.println("│ Total Checks: " + checkCount.get());
        System.out.println("└───────────────────────────────────────────────────────────────┘");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Extract experiment ID from method arguments
     */
    private String extractExperimentId(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof RunPlan) {
                return ((RunPlan) arg).getRunId();
            } else if (arg instanceof ExperimentDefinition) {
                return ((ExperimentDefinition) arg).getId();
            }
        }
        return null;
    }

    /**
     * Send alert when blast radius breach detected
     */
    private void sendAlert(
        String monitoringId,
        String experimentId,
        BlastRadiusService.ValidationResult result
    ) {
        System.out.println("   📢 ALERT: Blast radius breach in monitoring session " + monitoringId);
        System.out.println("      Experiment: " + experimentId);
        System.out.println("      Breaches: " + result.getBreaches().size());
        // TODO: Integrate with alerting systems (PagerDuty, Slack, etc.)
    }

    /**
     * Cleanup on bean destruction
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
     * Exception thrown when blast radius exceeds safety thresholds
     */
    public static class BlastRadiusExceededException extends RuntimeException {
        private final String experimentId;
        private final List<BlastRadiusService.BlastRadiusBreach> breaches;

        public BlastRadiusExceededException(
            String message,
            String experimentId,
            List<BlastRadiusService.BlastRadiusBreach> breaches
        ) {
            super(message);
            this.experimentId = experimentId;
            this.breaches = breaches;
        }

        public String getExperimentId() {
            return experimentId;
        }

        public List<BlastRadiusService.BlastRadiusBreach> getBreaches() {
            return breaches;
        }
    }
}

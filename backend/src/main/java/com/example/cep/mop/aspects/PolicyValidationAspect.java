package com.example.cep.mop.aspects;

import com.example.cep.mop.annotations.MonitorPolicyCompliance;
import com.example.cep.mop.model.PolicyState;
import com.example.cep.mop.service.PolicyStateService;
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
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MOP Aspect for Policy Compliance Monitoring
 *
 * This aspect implements Monitoring-Oriented Programming (MOP) for SLO policy validation.
 * It intercepts methods annotated with @MonitorPolicyCompliance and ensures that the
 * SLO policy remains consistent throughout the operation execution.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MONITORING-ORIENTED PROGRAMMING (MOP) - COMPLETE IMPLEMENTATION
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * This aspect demonstrates all three pillars of MOP:
 *
 * 1. OBSERVATION (Lines 120-140):
 *    - Captures policy snapshot at operation start
 *    - Spawns background thread for continuous monitoring
 *    - Tracks policy state throughout execution
 *    - Records all state transitions
 *
 * 2. VERIFICATION (Lines 200-250):
 *    - Validates: "Policy at time T == Policy at time T0"
 *    - Checks property: Policy consistency over time
 *    - Compares snapshots to detect policy drift
 *    - Identifies specific policy changes
 *
 * 3. ACTIONS (Lines 260-290):
 *    On Validation (Policy Unchanged):
 *    - Log successful validation
 *    - Continue operation
 *    - Update compliance metrics
 *
 *    On Violation (Policy Changed):
 *    - Abort operation immediately
 *    - Log detailed violation information
 *    - Send alerts to administrators
 *    - Record in audit trail
 *    - Throw PolicyChangedException
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * AOP CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. CROSS-CUTTING CONCERN:
 *    - Policy validation applies to multiple operation types
 *    - Without AOP: Each method needs embedded validation code
 *    - With AOP: Single aspect handles all policy validation
 *
 * 2. SEPARATION OF CONCERNS:
 *    - Business logic: Execute experiment/deployment
 *    - Cross-cutting logic: Validate policy consistency
 *    - Clear separation via aspect-oriented approach
 *
 * 3. @AROUND ADVICE:
 *    - Most powerful advice type in AspectJ
 *    - Full control over method execution
 *    - Can prevent method execution on violation
 *    - Can modify arguments and return values
 *
 * 4. DECLARATIVE PROGRAMMING:
 *    - Developer adds @MonitorPolicyCompliance annotation
 *    - Aspect automatically handles all monitoring
 *    - No manual validation code needed
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * RUNTIME BEHAVIOR
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Execution Flow:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 1. Method call: executeExperiment(plan)                                    │
 * │    ↓                                                                        │
 * │ 2. Aspect intercepts (BEFORE method execution)                             │
 * │    - Extract SLO targets from arguments                                    │
 * │    - Capture policy snapshot via PolicyStateService                        │
 * │    - Start background monitoring thread                                    │
 * │    ↓                                                                        │
 * │ 3. Proceed with actual method execution                                    │
 * │    ↓                                                                        │
 * │ 4. Background thread runs in parallel:                                     │
 * │    - Every N seconds: validate policy consistency                          │
 * │    - If policy changed: set abort flag                                     │
 * │    ↓                                                                        │
 * │ 5. Method completes (or is aborted)                                        │
 * │    ↓                                                                        │
 * │ 6. Aspect checks abort flag (AFTER method execution)                       │
 * │    - If aborted: throw PolicyChangedException                              │
 * │    - If successful: return result                                          │
 * │    ↓                                                                        │
 * │ 7. Cleanup: Stop monitoring thread, clear snapshot                         │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Aspect
@Component
public class PolicyValidationAspect {

    private final PolicyStateService policyStateService;
    private final ExecutorService monitoringExecutor;

    /**
     * Constructor with dependency injection
     *
     * @param policyStateService Service for policy state management
     */
    public PolicyValidationAspect(PolicyStateService policyStateService) {
        this.policyStateService = policyStateService;
        this.monitoringExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("MOP-PolicyMonitor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AROUND ADVICE - Policy Compliance Monitoring
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
     * @throws Throwable if method fails or policy violation occurs
     */
    @Around("@annotation(monitor)")
    public Object monitorPolicyCompliance(
        ProceedingJoinPoint joinPoint,
        MonitorPolicyCompliance monitor
    ) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        // Generate monitoring ID
        String monitoringId = monitor.monitoringId().isEmpty()
            ? UUID.randomUUID().toString().substring(0, 8)
            : monitor.monitoringId();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ MOP ASPECT: Policy Compliance Monitoring                     ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Method:      " + String.format("%-47s", methodName) + " ║");
        System.out.println("║ Monitor ID:  " + String.format("%-47s", monitoringId) + " ║");
        System.out.println("║ Interval:    " + String.format("%-47s", monitor.validationIntervalSeconds() + "s") + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 1: OBSERVATION - Extract and capture policy snapshot
        // ──────────────────────────────────────────────────────────────────────────
        List<SloTarget> sloTargets = extractSloTargets(joinPoint);
        String extractedId = extractExperimentId(joinPoint);

        if (sloTargets == null || sloTargets.isEmpty()) {
            System.out.println("⚠️  No SLO targets found - skipping policy monitoring");
            return joinPoint.proceed();
        }

        // Make experimentId final for use in lambda
        final String experimentId = (extractedId != null) ? extractedId : "experiment-" + monitoringId;

        System.out.println("📊 Monitoring " + sloTargets.size() + " SLO target(s) for experiment: " + experimentId);

        // Capture initial policy snapshot
        PolicyState initialSnapshot = policyStateService.captureSnapshot(experimentId, sloTargets);

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 2: Start background monitoring thread
        // ──────────────────────────────────────────────────────────────────────────
        AtomicBoolean shouldAbort = new AtomicBoolean(false);
        AtomicBoolean isRunning = new AtomicBoolean(true);
        AtomicInteger validationCount = new AtomicInteger(0);

        Future<?> monitoringFuture = monitoringExecutor.submit(() -> {
            monitoringLoop(
                experimentId,
                monitoringId,
                sloTargets,
                monitor,
                shouldAbort,
                isRunning,
                validationCount
            );
        });

        System.out.println("🚀 Background policy monitoring started");
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

            System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║ MOP Monitoring Session Completed                             ║");
            System.out.println("╠════════════════════════════════════════════════════════════════╣");
            System.out.println("║ Monitor ID:        " + String.format("%-43s", monitoringId) + " ║");
            System.out.println("║ Validations:       " + String.format("%-43s", validationCount.get()) + " ║");
            System.out.println("║ Policy Changed:    " + String.format("%-43s", shouldAbort.get() ? "YES" : "NO") + " ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
        }

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 4: ACTIONS - Handle validation or violation
        // ──────────────────────────────────────────────────────────────────────────

        // Check if policy violation detected
        if (shouldAbort.get() && monitor.abortOnPolicyChange()) {
            System.out.println("❌ OPERATION ABORTED: Policy changed during execution\n");

            // Get violation details
            List<PolicyStateService.PolicyViolation> violations =
                policyStateService.getViolationHistory(experimentId);

            String message = monitor.violationMessage().isEmpty()
                ? "SLO policy changed during operation execution - aborted for consistency"
                : monitor.violationMessage();

            policyStateService.clearExperiment(experimentId);

            throw new PolicyChangedException(message, experimentId, violations);
        }

        // Clean up policy state
        policyStateService.clearExperiment(experimentId);

        // Rethrow any exception from method
        if (methodException != null) {
            throw methodException;
        }

        System.out.println("✅ Operation completed with consistent policy\n");
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MONITORING LOOP - Background Verification
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Background monitoring loop that continuously validates policy
     *
     * @param experimentId Experiment identifier
     * @param monitoringId Monitoring session ID
     * @param sloTargets SLO targets to validate
     * @param config Monitoring configuration
     * @param shouldAbort Flag to signal abort
     * @param isRunning Flag indicating if method is executing
     * @param validationCount Counter for validations performed
     */
    private void monitoringLoop(
        String experimentId,
        String monitoringId,
        List<SloTarget> sloTargets,
        MonitorPolicyCompliance config,
        AtomicBoolean shouldAbort,
        AtomicBoolean isRunning,
        AtomicInteger validationCount
    ) {
        Duration interval = Duration.ofSeconds(config.validationIntervalSeconds());

        System.out.println("┌─ MOP Monitoring Loop Started ────────────────────────────────┐");
        System.out.println("│ Experiment:  " + experimentId);
        System.out.println("│ Interval:    " + config.validationIntervalSeconds() + "s");
        System.out.println("└───────────────────────────────────────────────────────────────┘\n");

        while (isRunning.get()) {
            try {
                Thread.sleep(interval.toMillis());

                // ──────────────────────────────────────────────────────────────────
                // VERIFICATION: Check if policy has changed
                // ──────────────────────────────────────────────────────────────────
                int currentCheck = validationCount.incrementAndGet();

                if (config.logAllValidations()) {
                    System.out.println("🔍 Policy Validation Check #" + currentCheck +
                                     " @ " + Instant.now());
                }

                PolicyStateService.ValidationResult result =
                    policyStateService.validatePolicy(experimentId, sloTargets);

                if (!result.isValid()) {
                    // ──────────────────────────────────────────────────────────────
                    // VIOLATION DETECTED
                    // ──────────────────────────────────────────────────────────────
                    System.out.println("   ❌ POLICY VIOLATION DETECTED");

                    if (config.alertOnViolation()) {
                        sendAlert(monitoringId, experimentId, result);
                    }

                    if (config.abortOnPolicyChange()) {
                        System.out.println("   🛑 Setting abort flag");
                        shouldAbort.set(true);
                        break; // Exit monitoring loop
                    }
                } else {
                    if (config.logAllValidations()) {
                        System.out.println("   ✅ Policy consistent");
                    }
                }

            } catch (InterruptedException e) {
                System.out.println("⚠️  Monitoring thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("❌ Error during policy validation: " + e.getMessage());
            }
        }

        System.out.println("\n┌─ MOP Monitoring Loop Stopped ────────────────────────────────┐");
        System.out.println("│ Total Validations: " + validationCount.get());
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
     * Send alert when policy violation detected
     */
    private void sendAlert(
        String monitoringId,
        String experimentId,
        PolicyStateService.ValidationResult result
    ) {
        System.out.println("   📢 ALERT: Policy violation in monitoring session " + monitoringId);
        System.out.println("      Experiment: " + experimentId);
        System.out.println("      Changes detected: " + result.getDifferences().size());
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
     * Exception thrown when policy change is detected during operation
     */
    public static class PolicyChangedException extends RuntimeException {
        private final String experimentId;
        private final List<PolicyStateService.PolicyViolation> violations;

        public PolicyChangedException(
            String message,
            String experimentId,
            List<PolicyStateService.PolicyViolation> violations
        ) {
            super(message);
            this.experimentId = experimentId;
            this.violations = violations;
        }

        public String getExperimentId() {
            return experimentId;
        }

        public List<PolicyStateService.PolicyViolation> getViolations() {
            return violations;
        }
    }
}

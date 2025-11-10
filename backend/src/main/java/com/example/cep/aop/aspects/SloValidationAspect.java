package com.example.cep.aop.aspects;

import com.example.cep.aop.annotations.ValidateSlo;
import com.example.cep.aop.annotations.ValidationPhase;
import com.example.cep.controlplane.service.SloEvaluator;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.RunPlan;
import com.example.cep.model.SloTarget;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Aspect-Oriented Programming (AOP) component for SLO validation
 *
 * This aspect implements cross-cutting concern of Service Level Objective (SLO) validation
 * across all chaos engineering experiment operations. It intercepts methods annotated with
 * @ValidateSlo and automatically validates that system metrics meet defined thresholds.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * AOP CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. CROSS-CUTTING CONCERN:
 *    - SLO validation spans multiple layers (API, Service, Orchestrator)
 *    - Traditional OOP would require duplicating validation code in each layer
 *    - AOP allows centralized, declarative validation via annotations
 *
 * 2. ASPECT:
 *    - This class is the aspect that encapsulates SLO validation logic
 *    - Declared with @Aspect annotation and registered as Spring @Component
 *
 * 3. JOIN POINTS:
 *    - Points in program execution where aspect can be applied
 *    - In this case: methods annotated with @ValidateSlo
 *
 * 4. ADVICE:
 *    - Action taken by aspect at a join point
 *    - @Before: Execute before method
 *    - @AfterReturning: Execute after successful method completion
 *    - @Around: Wrap method execution (control when/if method executes)
 *
 * 5. POINTCUT:
 *    - Expression that selects join points where advice should apply
 *    - Example: @annotation(validateSlo) selects methods with @ValidateSlo
 *
 * 6. WEAVING:
 *    - Process of applying aspects to target objects
 *    - AspectJ Maven plugin performs compile-time weaving
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * INTEGRATION WITH CHAOS ENGINEERING PLATFORM
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * This aspect integrates with:
 * - SloEvaluator: Queries Prometheus and evaluates SLO thresholds
 * - OrchestratorService: Coordinates experiment execution and abort logic
 * - RunPlan/ExperimentDefinition: Contains SLO targets to validate
 *
 * Workflow:
 * 1. Developer calls OrchestratorService.dispatch(plan)
 * 2. @ValidateSlo annotation on dispatch() triggers this aspect
 * 3. Aspect extracts SLO targets from plan.getDefinition().getSlos()
 * 4. SloEvaluator queries Prometheus for current metrics
 * 5. If SLO breach detected and abortOnBreach=true, throw exception
 * 6. Otherwise, proceed with experiment execution
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * SOA/ENTERPRISE ARCHITECTURE PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * - Service Abstraction: SLO validation abstracted behind annotations
 * - Service Reusability: Single aspect reused across all experiments
 * - Service Loose Coupling: Aspect depends only on SloEvaluator interface
 * - Service Autonomy: Aspect independently manages SLO validation lifecycle
 * - Policy Centralization: All SLO validation rules in one aspect
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Aspect
@Component
public class SloValidationAspect {

    private final SloEvaluator sloEvaluator;

    /**
     * Constructor-based dependency injection
     *
     * @param sloEvaluator Service for evaluating SLO metrics
     */
    public SloValidationAspect(SloEvaluator sloEvaluator) {
        this.sloEvaluator = sloEvaluator;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BEFORE ADVICE - Validates SLOs before method execution
    // ═══════════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════════
    // AROUND ADVICE - Most powerful advice type, wraps method execution
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Pointcut + Advice: Comprehensive SLO validation with full control
     *
     * This is the most powerful advice type in AspectJ. It can:
     * - Execute code before the method
     * - Decide whether to proceed with method execution
     * - Modify method arguments
     * - Execute code after the method
     * - Modify the return value
     * - Handle exceptions
     *
     * This advice handles all validation phases by checking the phase at runtime.
     * Spring AOP uses runtime weaving, so we can check annotation values dynamically.
     *
     * Control Flow:
     * 1. Extract SLO targets from method arguments
     * 2. Evaluate baseline SLOs (BEFORE)
     * 3. If breach detected and abortOnBreach=true, throw exception (ABORT)
     * 4. Otherwise, proceed with method execution
     * 5. Evaluate recovery SLOs (AFTER) if phase is AROUND_EXECUTION
     * 6. Return result or throw exception
     *
     * @param joinPoint Provides access to method context (args, signature, etc.)
     * @param validateSlo The @ValidateSlo annotation instance
     * @return The return value from the wrapped method
     * @throws Throwable if method throws exception or SLO breach occurs
     */
    @Around("@annotation(validateSlo)")
    public Object validateAroundExecution(ProceedingJoinPoint joinPoint, ValidateSlo validateSlo) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        ValidationPhase phase = validateSlo.phase();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ AOP ASPECT: SLO Validation (AROUND Advice)                   ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Method: " + String.format("%-52s", methodName) + " ║");
        System.out.println("║ Phase:  " + String.format("%-52s", phase) + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 1: Extract SLO targets from method arguments
        // ──────────────────────────────────────────────────────────────────────────
        List<SloTarget> sloTargets = extractSloTargets(joinPoint);

        if (sloTargets == null || sloTargets.isEmpty()) {
            System.out.println("⚠️  No SLO targets found in method arguments - skipping validation");
            return joinPoint.proceed();
        }

        System.out.println("📊 Found " + sloTargets.size() + " SLO target(s) to validate");

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 2: BEFORE - Validate baseline SLOs (for all phases except AFTER_EXECUTION)
        // ──────────────────────────────────────────────────────────────────────────
        if (phase != ValidationPhase.AFTER_EXECUTION) {
            System.out.println("\n┌─ BEFORE Execution: Baseline SLO Validation ─────────────────┐");
            Map<String, Object> baselineResults = sloEvaluator.evaluate(sloTargets);
            boolean baselineBreach = sloEvaluator.breaches(baselineResults);

            if (validateSlo.logResults()) {
                logSloResults("BASELINE", baselineResults);
            }

            if (baselineBreach) {
                String message = validateSlo.breachMessage().isEmpty()
                    ? "SLO breach detected in baseline state - experiment aborted"
                    : validateSlo.breachMessage();

                System.out.println("└──────────────────────────────────────────────────────────────┘");
                System.out.println("❌ BASELINE SLO BREACH DETECTED");

                if (validateSlo.abortOnBreach()) {
                    System.out.println("🛑 Aborting experiment due to baseline SLO breach");
                    throw new SloBreachException(message, baselineResults);
                } else {
                    System.out.println("⚠️  Proceeding despite baseline breach (abortOnBreach=false)");
                }
            } else {
                System.out.println("✅ Baseline SLOs validated successfully");
            }
            System.out.println("└──────────────────────────────────────────────────────────────┘");
        }

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 3: PROCEED - Execute the actual method
        // ──────────────────────────────────────────────────────────────────────────
        System.out.println("\n⚙️  Proceeding with method execution...\n");
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            System.out.println("❌ Method execution failed: " + e.getMessage());
            throw e;
        }

        // ──────────────────────────────────────────────────────────────────────────
        // STEP 4: AFTER - Validate recovery SLOs (for AROUND_EXECUTION and AFTER_EXECUTION)
        // ──────────────────────────────────────────────────────────────────────────
        if (phase == ValidationPhase.AROUND_EXECUTION || phase == ValidationPhase.AFTER_EXECUTION) {
            System.out.println("┌─ AFTER Execution: Recovery SLO Validation ──────────────────┐");
            Map<String, Object> recoveryResults = sloEvaluator.evaluate(sloTargets);
            boolean recoveryBreach = sloEvaluator.breaches(recoveryResults);

            if (validateSlo.logResults()) {
                logSloResults("RECOVERY", recoveryResults);
            }

            if (recoveryBreach) {
                System.out.println("└──────────────────────────────────────────────────────────────┘");
                System.out.println("❌ RECOVERY SLO BREACH DETECTED");
                System.out.println("⚠️  System did not recover to acceptable SLO levels");

                if (validateSlo.abortOnBreach()) {
                    throw new SloBreachException("System failed to recover after experiment", recoveryResults);
                }
            } else {
                System.out.println("✅ Recovery SLOs validated successfully");
                System.out.println("└──────────────────────────────────────────────────────────────┘");
            }
        }

        System.out.println("\n✅ SLO validation completed successfully\n");
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Extracts SLO targets from method arguments
     *
     * Searches through method arguments to find ExperimentDefinition or RunPlan
     * and extracts the SLO targets list.
     *
     * @param joinPoint Join point containing method arguments
     * @return List of SLO targets, or null if not found
     */
    private List<SloTarget> extractSloTargets(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof RunPlan) {
                RunPlan plan = (RunPlan) arg;
                return plan.getDefinition().getSlos();
            } else if (arg instanceof ExperimentDefinition) {
                ExperimentDefinition definition = (ExperimentDefinition) arg;
                return definition.getSlos();
            }
        }

        return null;
    }

    /**
     * Logs SLO evaluation results in a formatted table
     *
     * @param phase Phase name (BASELINE or RECOVERY)
     * @param results SLO evaluation results
     */
    private void logSloResults(String phase, Map<String, Object> results) {
        System.out.println("│");
        System.out.println("│ " + phase + " SLO Results:");

        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String key = entry.getKey();
            // Skip threshold and comparator entries (they'll be shown with their metric)
            if (key.contains("_threshold") || key.contains("_comparator") || key.contains("_error")) {
                continue;
            }

            Object value = entry.getValue();
            Object threshold = results.get(key + "_threshold");
            Object comparator = results.get(key + "_comparator");

            if (threshold != null && comparator != null) {
                System.out.println(String.format("│   %-20s = %-10s (threshold: %s %s)",
                    key, value, comparator, threshold));
            } else {
                System.out.println(String.format("│   %-20s = %s", key, value));
            }
        }
        System.out.println("│");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // EXCEPTION CLASS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Custom exception thrown when SLO breach is detected
     *
     * Contains both error message and detailed SLO evaluation results
     * for comprehensive error reporting and debugging.
     */
    public static class SloBreachException extends RuntimeException {
        private final Map<String, Object> sloResults;

        public SloBreachException(String message, Map<String, Object> sloResults) {
            super(message);
            this.sloResults = sloResults;
        }

        public Map<String, Object> getSloResults() {
            return sloResults;
        }
    }
}

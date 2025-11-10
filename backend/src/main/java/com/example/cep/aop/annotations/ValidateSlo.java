package com.example.cep.aop.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require SLO validation
 *
 * This annotation enables Aspect-Oriented Programming (AOP) for Service Level Objective (SLO) validation.
 * When applied to a method, the SloValidationAspect will automatically:
 *
 * 1. Validate baseline SLOs before method execution
 * 2. Monitor SLOs during method execution
 * 3. Verify SLOs after method completion
 * 4. Trigger abort logic if SLOs are breached
 *
 * Cross-Cutting Concern:
 * SLO validation is a cross-cutting concern that spans multiple layers and components.
 * Without AOP, we would need to duplicate SLO validation logic in every service method,
 * leading to code duplication and maintenance challenges.
 *
 * AOP Benefits:
 * - Separation of Concerns: Business logic separated from SLO validation logic
 * - Reusability: Single aspect applies to multiple methods via annotation
 * - Maintainability: SLO validation logic centralized in one aspect
 * - Non-invasive: No need to modify existing business logic
 *
 * Usage Example:
 * <pre>
 * {@code
 * @ValidateSlo(phase = ValidationPhase.BEFORE_EXECUTION, abortOnBreach = true)
 * public String dispatch(RunPlan plan) {
 *     // Business logic here
 * }
 * }
 * </pre>
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateSlo {

    /**
     * Defines when SLO validation should occur
     *
     * @return validation phase (default: BEFORE_EXECUTION)
     */
    ValidationPhase phase() default ValidationPhase.BEFORE_EXECUTION;

    /**
     * Whether to abort the operation if SLO breach is detected
     *
     * @return true to abort on breach (default: true)
     */
    boolean abortOnBreach() default true;

    /**
     * Custom error message when SLO breach occurs
     *
     * @return error message (default: empty string for default message)
     */
    String breachMessage() default "";

    /**
     * Whether to log SLO validation results
     *
     * @return true to log results (default: true)
     */
    boolean logResults() default true;
}

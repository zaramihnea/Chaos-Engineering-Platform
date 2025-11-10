package com.example.cep.aop.annotations;

/**
 * Defines the phase when SLO validation should occur
 *
 * This enum is used in conjunction with @ValidateSlo annotation to specify
 * at what point in the method execution lifecycle SLO validation should run.
 *
 * Validation Phases:
 *
 * 1. BEFORE_EXECUTION:
 *    - Validates that system meets baseline SLOs before starting experiment
 *    - Prevents experiments from running on already degraded systems
 *    - Use case: Pre-flight checks before chaos injection
 *
 * 2. AFTER_EXECUTION:
 *    - Validates that system recovered to acceptable SLO levels after experiment
 *    - Ensures chaos experiments don't leave system in degraded state
 *    - Use case: Post-experiment validation and cleanup verification
 *
 * 3. AROUND_EXECUTION:
 *    - Validates SLOs both before and after method execution
 *    - Comprehensive validation for critical operations
 *    - Use case: High-stakes experiments requiring full validation cycle
 *
 * 4. CONTINUOUS:
 *    - Monitors SLOs continuously during method execution
 *    - Enables real-time detection and abort during long-running experiments
 *    - Use case: Active monitoring during chaos injection
 *
 * AOP Mapping:
 * - BEFORE_EXECUTION  → @Before advice
 * - AFTER_EXECUTION   → @AfterReturning advice
 * - AROUND_EXECUTION  → @Around advice
 * - CONTINUOUS        → @Around advice with periodic checks
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
public enum ValidationPhase {

    /**
     * Validate SLOs before method execution
     * Maps to AspectJ @Before advice
     */
    BEFORE_EXECUTION,

    /**
     * Validate SLOs after successful method execution
     * Maps to AspectJ @AfterReturning advice
     */
    AFTER_EXECUTION,

    /**
     * Validate SLOs both before and after method execution
     * Maps to AspectJ @Around advice
     */
    AROUND_EXECUTION,

    /**
     * Continuously monitor SLOs during method execution
     * Maps to AspectJ @Around advice with periodic polling
     */
    CONTINUOUS
}

package com.example.cep.aop.aspects;

import com.example.cep.aop.annotations.ValidateSlo;
import com.example.cep.aop.annotations.ValidationPhase;
import com.example.cep.controlplane.service.SloEvaluator;
import com.example.cep.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for SloValidationAspect
 *
 * These tests verify that the AOP aspect correctly intercepts methods annotated
 * with @ValidateSlo and performs SLO validation at the appropriate lifecycle points.
 *
 * Test Strategy:
 * - Create a test service with @ValidateSlo annotated methods
 * - Mock the SloEvaluator to control SLO evaluation results
 * - Invoke the test service methods
 * - Verify that aspect intercepts calls and validates SLOs
 * - Assert that exceptions are thrown when SLO breaches occur
 *
 * AOP Testing Challenges:
 * - Aspects only work on Spring-managed beans (not direct instantiation)
 * - Must use @SpringBootTest to load full application context
 * - Cannot test aspect in isolation - must test integration with target beans
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@SpringBootTest
@DisplayName("SLO Validation Aspect Integration Tests")
class SloValidationAspectTest {

    @Autowired
    private TestExperimentService testService;

    @MockBean
    private SloEvaluator sloEvaluator;

    private RunPlan testRunPlan;
    private Map<String, Object> mockSloResults;

    @BeforeEach
    void setUp() {
        // Create test SLO targets
        List<SloTarget> sloTargets = Arrays.asList(
            new SloTarget(SloMetric.LATENCY_P95, "latency_query", 500.0, "<"),
            new SloTarget(SloMetric.ERROR_RATE, "error_query", 0.01, "<")
        );

        // Create test experiment definition
        ExperimentDefinition definition = new ExperimentDefinition(
            "test-exp-1",
            "Test Experiment",
            FaultType.POD_KILL,
            Map.of("podName", "test-pod"),
            new TargetSystem("default", "test-app", Map.of("app", "test")),
            Duration.ofMinutes(5),
            sloTargets,
            true,
            "test-user"
        );

        // Create test run plan
        testRunPlan = new RunPlan(
            "run-123",
            definition,
            Instant.now(),
            false
        );

        // Create mock SLO results
        mockSloResults = new HashMap<>();
        mockSloResults.put("latency_p95", 250.0);
        mockSloResults.put("latency_p95_threshold", 500.0);
        mockSloResults.put("latency_p95_comparator", "<");
        mockSloResults.put("error_rate", 0.005);
        mockSloResults.put("error_rate_threshold", 0.01);
        mockSloResults.put("error_rate_comparator", "<");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BEFORE_EXECUTION PHASE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should validate SLOs before execution - baseline meets threshold")
    void testValidateBeforeExecution_BaselineMeetsThreshold_Success() {
        // Arrange: Mock SLO evaluator to return good baseline
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any())).thenReturn(false);

        // Act: Call method with @ValidateSlo(BEFORE_EXECUTION)
        String result = testService.executeWithBeforeValidation(testRunPlan);

        // Assert: Method should complete successfully
        assertNotNull(result);
        assertEquals("Execution completed", result);

        // Verify SLO evaluation was called
        verify(sloEvaluator, times(1)).evaluate(any());
        verify(sloEvaluator, times(1)).breaches(any());
    }

    @Test
    @DisplayName("Should abort before execution when baseline SLO breached")
    void testValidateBeforeExecution_BaselineBreached_ThrowsException() {
        // Arrange: Mock SLO evaluator to return baseline breach
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any())).thenReturn(true);

        // Act & Assert: Method should throw SloBreachException
        assertThrows(
            SloValidationAspect.SloBreachException.class,
            () -> testService.executeWithBeforeValidation(testRunPlan),
            "Should throw exception when baseline SLO is breached"
        );

        // Verify SLO evaluation was called
        verify(sloEvaluator, times(1)).evaluate(any());
        verify(sloEvaluator, times(1)).breaches(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AROUND_EXECUTION PHASE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should validate SLOs around execution - both baseline and recovery pass")
    void testValidateAroundExecution_AllSlosPass_Success() {
        // Arrange: Mock SLO evaluator to return good results for both checks
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any())).thenReturn(false);

        // Act: Call method with @ValidateSlo(AROUND_EXECUTION)
        String result = testService.executeWithAroundValidation(testRunPlan);

        // Assert: Method should complete successfully
        assertNotNull(result);
        assertEquals("Execution completed", result);

        // Verify SLO evaluation was called twice (before + after)
        verify(sloEvaluator, times(2)).evaluate(any());
        verify(sloEvaluator, times(2)).breaches(any());
    }

    @Test
    @DisplayName("Should abort when baseline breached in around execution")
    void testValidateAroundExecution_BaselineBreached_ThrowsException() {
        // Arrange: Mock SLO evaluator to return baseline breach
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any())).thenReturn(true);

        // Act & Assert: Should throw exception before method executes
        assertThrows(
            SloValidationAspect.SloBreachException.class,
            () -> testService.executeWithAroundValidation(testRunPlan),
            "Should throw exception when baseline SLO is breached"
        );

        // Verify only one evaluation (baseline check failed, recovery not reached)
        verify(sloEvaluator, times(1)).evaluate(any());
        verify(sloEvaluator, times(1)).breaches(any());
    }

    @Test
    @DisplayName("Should detect recovery failure after execution")
    void testValidateAroundExecution_RecoveryFailed_ThrowsException() {
        // Arrange: First call (baseline) passes, second call (recovery) fails
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any()))
            .thenReturn(false)  // Baseline passes
            .thenReturn(true);  // Recovery fails

        // Act & Assert: Should throw exception after method executes
        assertThrows(
            SloValidationAspect.SloBreachException.class,
            () -> testService.executeWithAroundValidation(testRunPlan),
            "Should throw exception when recovery SLO is breached"
        );

        // Verify both evaluations were called
        verify(sloEvaluator, times(2)).evaluate(any());
        verify(sloEvaluator, times(2)).breaches(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ABORT CONFIGURATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should NOT abort when abortOnBreach=false despite SLO breach")
    void testValidateWithoutAbort_BreachOccurs_ContinuesExecution() {
        // Arrange: Mock SLO evaluator to return breach
        when(sloEvaluator.evaluate(any())).thenReturn(mockSloResults);
        when(sloEvaluator.breaches(any())).thenReturn(true);

        // Act: Call method with abortOnBreach=false
        String result = testService.executeWithoutAbort(testRunPlan);

        // Assert: Method should complete despite breach
        assertNotNull(result);
        assertEquals("Execution completed", result);

        // Verify SLO evaluation was still called
        verify(sloEvaluator, atLeastOnce()).evaluate(any());
        verify(sloEvaluator, atLeastOnce()).breaches(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // EDGE CASE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should handle method with no SLO targets gracefully")
    void testValidateWithoutSloTargets_SkipsValidation() {
        // Arrange: Create plan with no SLO targets
        ExperimentDefinition definitionWithoutSlos = new ExperimentDefinition(
            "test-exp-2",
            "Test Without SLOs",
            FaultType.POD_KILL,
            Map.of(),
            new TargetSystem("default", "test-app", Map.of()),
            Duration.ofMinutes(5),
            Collections.emptyList(),  // No SLO targets
            true,
            "test-user"
        );
        RunPlan planWithoutSlos = new RunPlan("run-456", definitionWithoutSlos, Instant.now(), false);

        // Act: Call method - should skip validation
        String result = testService.executeWithBeforeValidation(planWithoutSlos);

        // Assert: Method should complete without calling SLO evaluator
        assertNotNull(result);
        verify(sloEvaluator, never()).evaluate(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST CONFIGURATION - Provides test beans
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test configuration to provide test service bean
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestExperimentService testExperimentService() {
            return new TestExperimentService();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST SERVICE - Bean to be proxied by AOP
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Test service with @ValidateSlo annotated methods
     *
     * This service is a Spring bean that will be proxied by the AOP framework.
     * When methods are called, the aspect intercepts and validates SLOs.
     */
    public static class TestExperimentService {

        @ValidateSlo(
            phase = ValidationPhase.BEFORE_EXECUTION,
            abortOnBreach = true,
            logResults = true
        )
        public String executeWithBeforeValidation(RunPlan plan) {
            return "Execution completed";
        }

        @ValidateSlo(
            phase = ValidationPhase.AROUND_EXECUTION,
            abortOnBreach = true,
            logResults = true
        )
        public String executeWithAroundValidation(RunPlan plan) {
            return "Execution completed";
        }

        @ValidateSlo(
            phase = ValidationPhase.AROUND_EXECUTION,
            abortOnBreach = false,  // Don't abort on breach
            logResults = true
        )
        public String executeWithoutAbort(RunPlan plan) {
            return "Execution completed";
        }
    }
}

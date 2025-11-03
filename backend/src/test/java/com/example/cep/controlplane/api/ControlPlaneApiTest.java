package com.example.cep.controlplane.api;

import com.example.cep.controlplane.service.PolicyService;
import com.example.cep.controlplane.service.OrchestratorService;
import com.example.cep.controlplane.store.ExperimentRepository;
import com.example.cep.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Iteration 1: Test-Driven Development for Experiment Creation & Policy Validation
 *
 * This test suite follows the RED-GREEN-REFACTOR cycle:
 * - RED: Write failing tests first (tests run before implementation exists)
 * - GREEN: Implement minimal code to make tests pass
 * - REFACTOR: Improve code quality while keeping tests green
 *
 * Test Coverage: 5 unit tests covering core experiment creation and policy validation
 * Mocking Strategy: Mock all external dependencies (Repository, PolicyService, OrchestratorService)
 * Architecture: Demonstrates Service-Oriented Architecture with clear separation of concerns
 */
class ControlPlaneApiTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private OrchestratorService orchestratorService;

    private ControlPlaneApi controlPlaneApi;

    /**
     * Setup method executed before each test
     * Initializes mocks and creates instance under test
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controlPlaneApi = new ControlPlaneApiImpl(experimentRepository, policyService, orchestratorService);
    }

    /**
     * TDD Iteration 1 - Test 1: Valid Experiment Creation
     *
     * RED Phase: This test will fail initially because ControlPlaneApiImpl doesn't exist yet
     * Expected failure: Class not found or method returns null
     */
    @Test
    @DisplayName("TDD-1.1: Create experiment with valid definition returns experiment ID")
    void testCreateExperiment_ValidDefinition_ReturnsId() {
        // ARRANGE: Prepare test data and mock behavior
        ExperimentDefinition validDefinition = createValidExperimentDefinition();

        // Mock policy service to allow the experiment
        when(policyService.isAllowed(any(ExperimentDefinition.class))).thenReturn(true);

        // ACT: Execute the method under test
        String experimentId = controlPlaneApi.createExperiment(validDefinition);

        // ASSERT: Verify the results
        assertNotNull(experimentId, "Experiment ID should not be null");
        assertFalse(experimentId.isEmpty(), "Experiment ID should not be empty");

        // Verify interactions with dependencies
        verify(policyService, times(1)).isAllowed(validDefinition);
        verify(experimentRepository, times(1)).saveDefinition(any(ExperimentDefinition.class));
    }

    /**
     * TDD Iteration 1 - Test 2: Policy Violation Handling
     *
     * RED Phase: This test will fail because exception handling is not implemented
     * Expected failure: No exception thrown or wrong exception type
     */
    @Test
    @DisplayName("TDD-1.2: Create experiment with policy violation throws PolicyViolationException")
    void testCreateExperiment_PolicyViolation_ThrowsException() {
        // ARRANGE: Prepare invalid experiment that violates policy
        ExperimentDefinition invalidDefinition = createInvalidExperimentDefinition();

        // Mock policy service to reject the experiment
        when(policyService.isAllowed(invalidDefinition)).thenReturn(false);
        when(policyService.denialReason(invalidDefinition)).thenReturn("Invalid namespace: production");

        // ACT & ASSERT: Verify exception is thrown
        PolicyViolationException exception = assertThrows(
            PolicyViolationException.class,
            () -> controlPlaneApi.createExperiment(invalidDefinition),
            "Should throw PolicyViolationException for policy violations"
        );

        // Verify exception contains meaningful message
        assertTrue(exception.getMessage().contains("Invalid namespace"),
            "Exception should contain policy denial reason");

        // Verify that save was never called
        verify(experimentRepository, never()).saveDefinition(any(ExperimentDefinition.class));
    }

    /**
     * TDD Iteration 1 - Test 3: Policy Validation for Allowed Experiment
     *
     * RED Phase: This test will fail because validatePolicy method doesn't delegate correctly
     * Expected failure: Method returns false or throws exception
     */
    @Test
    @DisplayName("TDD-1.3: Validate policy for allowed experiment returns true")
    void testValidatePolicy_AllowedExperiment_ReturnsTrue() {
        // ARRANGE: Create experiment that meets all policy requirements
        ExperimentDefinition allowedDefinition = createValidExperimentDefinition();

        // Mock policy service to allow the experiment
        when(policyService.isAllowed(allowedDefinition)).thenReturn(true);

        // ACT: Execute policy validation
        boolean isValid = controlPlaneApi.validatePolicy(allowedDefinition);

        // ASSERT: Verify policy validation passed
        assertTrue(isValid, "Policy validation should return true for allowed experiments");

        // Verify policy service was consulted
        verify(policyService, times(1)).isAllowed(allowedDefinition);
    }

    /**
     * TDD Iteration 1 - Test 4: Policy Validation for Disallowed Experiment
     *
     * RED Phase: This test will fail because policy rules are not implemented
     * Expected failure: Method returns true instead of false
     */
    @Test
    @DisplayName("TDD-1.4: Validate policy for disallowed experiment returns false")
    void testValidatePolicy_DisallowedExperiment_ReturnsFalse() {
        // ARRANGE: Create experiment that violates policy
        ExperimentDefinition disallowedDefinition = createInvalidExperimentDefinition();

        // Mock policy service to reject the experiment
        when(policyService.isAllowed(disallowedDefinition)).thenReturn(false);

        // ACT: Execute policy validation
        boolean isValid = controlPlaneApi.validatePolicy(disallowedDefinition);

        // ASSERT: Verify policy validation failed
        assertFalse(isValid, "Policy validation should return false for disallowed experiments");

        // Verify policy service was consulted
        verify(policyService, times(1)).isAllowed(disallowedDefinition);
    }

    /**
     * TDD Iteration 1 - Test 5: List All Experiments
     *
     * RED Phase: This test will fail because listExperiments returns null or empty
     * Expected failure: Returns null or incorrect number of experiments
     */
    @Test
    @DisplayName("TDD-1.5: List experiments returns all stored experiments")
    void testListExperiments_ReturnsAllExperiments() {
        // ARRANGE: Create multiple experiment definitions
        List<ExperimentDefinition> expectedExperiments = Arrays.asList(
            createValidExperimentDefinition(),
            createValidExperimentDefinition(),
            createValidExperimentDefinition()
        );

        // Mock repository to return the experiment list
        when(experimentRepository.findAll()).thenReturn(expectedExperiments);

        // ACT: Execute list experiments
        List<ExperimentDefinition> actualExperiments = controlPlaneApi.listExperiments();

        // ASSERT: Verify all experiments are returned
        assertNotNull(actualExperiments, "Experiment list should not be null");
        assertEquals(3, actualExperiments.size(), "Should return all 3 experiments");
        assertEquals(expectedExperiments, actualExperiments, "Should return the same experiment list");

        // Verify repository interaction
        verify(experimentRepository, times(1)).findAll();
    }

    // ==================== Test Helper Methods ====================

    /**
     * Creates a valid experiment definition for testing
     * Meets all policy requirements
     */
    private ExperimentDefinition createValidExperimentDefinition() {
        TargetSystem target = new TargetSystem(
            "staging-cluster",  // Valid cluster
            "staging",          // Valid namespace
            Map.of("app", "test-service")
        );

        SloTarget sloTarget = new SloTarget(
            SloMetric.LATENCY_P95,
            "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            500.0,  // threshold in ms
            "<"     // latency should be less than threshold
        );

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("duration", 300);  // 5 minutes (within 30 min limit)
        parameters.put("severity", "medium");

        ExperimentDefinition definition = new ExperimentDefinition(
            null,  // ID will be generated
            "Test Pod Kill Experiment",
            FaultType.POD_KILL,
            parameters,
            target,
            Duration.ofSeconds(300),  // 5 minutes timeout
            List.of(sloTarget),
            true,  // dry run allowed
            "tester@example.com"
        );

        return definition;
    }

    /**
     * Creates an invalid experiment definition that violates policy
     * Used for testing policy rejection scenarios
     */
    private ExperimentDefinition createInvalidExperimentDefinition() {
        TargetSystem target = new TargetSystem(
            "invalid-cluster",  // Invalid cluster (not in allowed list)
            "production",       // Invalid namespace (not in allowed list)
            Map.of("app", "critical-service")
        );

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("duration", 2400);  // 40 minutes (exceeds 30 min limit)

        ExperimentDefinition definition = new ExperimentDefinition(
            null,
            "Invalid Experiment",
            FaultType.NETWORK_PARTITION,  // Restricted fault type
            parameters,
            target,
            Duration.ofSeconds(2400),  // Exceeds max duration (40 minutes)
            List.of(),  // No SLOs defined (violates policy)
            false,
            "tester@example.com"
        );

        return definition;
    }
}

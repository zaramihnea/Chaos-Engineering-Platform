package com.example.cep.controlplane.api;

import com.example.cep.controlplane.service.OrchestratorService;
import com.example.cep.controlplane.service.PolicyService;
import com.example.cep.controlplane.store.ExperimentRepository;
import com.example.cep.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Control Plane API Implementation
 *
 * TDD Iteration 1 - GREEN Phase Implementation
 *
 * This implementation follows Service-Oriented Architecture principles:
 * - Service Abstraction: Implements ControlPlaneApi interface
 * - Loose Coupling: Dependencies injected via constructor
 * - Service Composability: Orchestrates multiple services (Policy, Repository, Orchestrator)
 * - Service Autonomy: Manages experiment lifecycle independently
 *
 * Responsibilities:
 * - Experiment creation with policy validation
 * - Experiment scheduling and execution coordination
 * - Report and state retrieval
 * - Experiment approval workflow
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0 (TDD Iteration 1)
 */
@Service
public class ControlPlaneApiImpl implements ControlPlaneApi {

    private final ExperimentRepository experimentRepository;
    private final PolicyService policyService;
    private final OrchestratorService orchestratorService;

    /**
     * Constructor-based dependency injection (Spring best practice)
     *
     * @param experimentRepository Repository for experiment persistence
     * @param policyService Service for policy validation
     * @param orchestratorService Service for experiment orchestration
     */
    public ControlPlaneApiImpl(
            ExperimentRepository experimentRepository,
            PolicyService policyService,
            OrchestratorService orchestratorService) {
        this.experimentRepository = experimentRepository;
        this.policyService = policyService;
        this.orchestratorService = orchestratorService;
    }

    /**
     * Creates a new chaos experiment definition
     *
     * TDD Test Coverage:
     * - testCreateExperiment_ValidDefinition_ReturnsId (GREEN)
     * - testCreateExperiment_PolicyViolation_ThrowsException (GREEN)
     *
     * Process:
     * 1. Validate experiment against organizational policies
     * 2. Generate unique experiment ID
     * 3. Persist experiment definition
     * 4. Return experiment ID for future reference
     *
     * @param def The experiment definition containing fault type, target, and SLOs
     * @return Unique experiment ID (UUID)
     * @throws PolicyViolationException if experiment violates organizational policies
     */
    @Override
    public String createExperiment(ExperimentDefinition def) {
        // Step 1: Policy Validation (Security Gate)
        if (!policyService.isAllowed(def)) {
            String reason = policyService.denialReason(def);
            throw new PolicyViolationException(
                "Experiment creation denied: " + reason +
                ". Please review your experiment configuration against organizational policies."
            );
        }

        // Step 2: Generate Unique Identifier
        String experimentId = UUID.randomUUID().toString();

        // Step 3: Set ID on definition and persist
        ExperimentDefinition definitionWithId = new ExperimentDefinition(
            experimentId,
            def.getName(),
            def.getFaultType(),
            def.getParameters(),
            def.getTarget(),
            def.getTimeout(),
            def.getSlos(),
            def.isDryRunAllowed(),
            def.getCreatedBy()
        );

        experimentRepository.saveDefinition(definitionWithId);

        // Step 4: Log and return
        System.out.println("Created experiment: " + experimentId + " (" + def.getName() + ")");
        return experimentId;
    }

    /**
     * Schedules an experiment run for execution
     *
     * Process:
     * 1. Verify experiment exists
     * 2. Create run plan with scheduling details
     * 3. Persist run plan
     * 4. Dispatch to orchestrator for execution
     *
     * @param experimentId The ID of the experiment to run
     * @param when Scheduled execution time (can be immediate)
     * @param dryRun If true, simulate without actual fault injection
     * @return Unique run ID for tracking execution
     * @throws ExperimentNotFoundException if experiment ID is invalid
     */
    @Override
    public String scheduleRun(String experimentId, Instant when, boolean dryRun) {
        // Step 1: Validate experiment exists
        ExperimentDefinition definition = experimentRepository.findById(experimentId);
        if (definition == null) {
            throw new ExperimentNotFoundException(
                "Cannot schedule run: Experiment with ID '" + experimentId + "' not found. " +
                "Please verify the experiment ID and try again."
            );
        }

        // Step 2: Validate dry run is allowed if requested
        if (dryRun && !definition.isDryRunAllowed()) {
            throw new PolicyViolationException(
                "Dry run not allowed for experiment: " + experimentId +
                ". This experiment requires live execution only."
            );
        }

        // Step 3: Create run plan
        String runId = UUID.randomUUID().toString();
        RunPlan plan = new RunPlan(runId, definition, when, dryRun);

        // Step 4: Persist and dispatch
        experimentRepository.saveRunPlan(plan);
        orchestratorService.dispatch(plan);

        System.out.println("Scheduled run: " + runId + " for experiment: " + experimentId +
                         " (dry-run: " + dryRun + ")");
        return runId;
    }

    /**
     * Aborts a running experiment
     *
     * @param runId The ID of the run to abort
     * @param reason Human-readable reason for abortion
     */
    @Override
    public void abortRun(String runId, String reason) {
        RunPlan plan = experimentRepository.findRunPlan(runId);
        if (plan == null) {
            throw new RunNotFoundException(
                "Cannot abort run: Run with ID '" + runId + "' not found."
            );
        }

        System.out.println("Aborting run: " + runId + " - Reason: " + reason);

        // Finalize run with ABORTED state
        orchestratorService.finalizeRun(runId, RunState.ABORTED);
    }

    /**
     * Retrieves the execution report for a completed run
     *
     * @param runId The ID of the run
     * @return Complete execution report with SLO metrics
     * @throws RunNotFoundException if run ID is invalid
     */
    @Override
    public Report getReport(String runId) {
        Report report = experimentRepository.findReport(runId);
        if (report == null) {
            throw new RunNotFoundException(
                "Report not found for run: " + runId +
                ". Run may still be executing or may not exist."
            );
        }
        return report;
    }

    /**
     * Retrieves the current state of a run
     *
     * @param runId The ID of the run
     * @return Current execution state
     * @throws RunNotFoundException if run ID is invalid
     */
    @Override
    public RunState getRunState(String runId) {
        RunPlan plan = experimentRepository.findRunPlan(runId);
        if (plan == null) {
            throw new RunNotFoundException(
                "Cannot get state: Run with ID '" + runId + "' not found."
            );
        }

        // In a full implementation, this would query the orchestrator for real-time state
        // For now, return a basic state based on plan existence
        return RunState.SCHEDULED;
    }

    /**
     * Lists all experiment definitions in the system
     *
     * TDD Test Coverage:
     * - testListExperiments_ReturnsAllExperiments (GREEN)
     *
     * @return List of all experiment definitions
     */
    @Override
    public List<ExperimentDefinition> listExperiments() {
        return experimentRepository.findAll();
    }

    /**
     * Deletes an experiment definition from the system
     *
     * @param experimentId The ID of the experiment to delete
     * @throws ExperimentNotFoundException if experiment ID is invalid
     */
    @Override
    public void deleteExperiment(String experimentId) {
        ExperimentDefinition definition = experimentRepository.findById(experimentId);
        if (definition == null) {
            throw new ExperimentNotFoundException(
                "Cannot delete: Experiment with ID '" + experimentId + "' not found."
            );
        }

        experimentRepository.deleteById(experimentId);
        System.out.println("Deleted experiment: " + experimentId + " (" + definition.getName() + ")");
    }

    /**
     * Approves an experiment for execution
     *
     * Used for experiments requiring elevated privileges (e.g., NETWORK_PARTITION)
     *
     * @param experimentId The experiment to approve
     * @param approver The identity of the approver
     * @return Approval ID for audit trail
     */
    @Override
    public String approveExperiment(String experimentId, String approver) {
        ExperimentDefinition definition = experimentRepository.findById(experimentId);
        if (definition == null) {
            throw new ExperimentNotFoundException(
                "Cannot approve: Experiment with ID '" + experimentId + "' not found."
            );
        }

        String approvalId = UUID.randomUUID().toString();
        System.out.println("Experiment " + experimentId + " approved by " + approver +
                         " (approval ID: " + approvalId + ")");
        return approvalId;
    }

    /**
     * Validates an experiment definition against organizational policies
     *
     * TDD Test Coverage:
     * - testValidatePolicy_AllowedExperiment_ReturnsTrue (GREEN)
     * - testValidatePolicy_DisallowedExperiment_ReturnsFalse (GREEN)
     *
     * @param def The experiment definition to validate
     * @return true if experiment passes all policy checks
     */
    @Override
    public boolean validatePolicy(ExperimentDefinition def) {
        return policyService.isAllowed(def);
    }

    @Override
    public List<RunPlan> getRunsForExperiment(String experimentId) {
        return experimentRepository.findRunsByExperimentId(experimentId);
    }
}

/**
 * Exception thrown when an experiment violates organizational policies
 *
 * Policy violations include:
 * - Invalid target namespace/cluster
 * - Experiment duration exceeds limits
 * - Missing required SLO definitions
 * - Restricted fault types without approval
 */
class PolicyViolationException extends RuntimeException {
    public PolicyViolationException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when an experiment is not found in the repository
 */
class ExperimentNotFoundException extends RuntimeException {
    public ExperimentNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when a run is not found in the repository
 */
class RunNotFoundException extends RuntimeException {
    public RunNotFoundException(String message) {
        super(message);
    }
}

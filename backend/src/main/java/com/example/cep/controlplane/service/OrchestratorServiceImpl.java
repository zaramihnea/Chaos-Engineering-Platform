package com.example.cep.controlplane.service;

import com.example.cep.aop.annotations.ValidateSlo;
import com.example.cep.aop.annotations.ValidationPhase;
import com.example.cep.controlplane.store.ExperimentRepository;
import com.example.cep.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrator Service Implementation
 *
 * TDD Iteration 2 - GREEN Phase Implementation
 *
 * This service orchestrates the execution of chaos experiments by:
 * 1. Dispatching run plans to agents
 * 2. Tracking execution state
 * 3. Processing agent updates
 * 4. Evaluating SLOs post-execution
 * 5. Generating comprehensive reports
 *
 * SOA Principles Demonstrated:
 * - Service Composability: Combines ExperimentRepository and SloEvaluator services
 * - Service Autonomy: Maintains independent run state
 * - Loose Coupling: Depends only on interfaces
 * - Service Reusability: Can orchestrate any experiment type
 *
 * Key Feature: SLO-based Safety Mechanism
 * If SLOs are breached during an experiment, the run is marked as FAILED
 * regardless of agent-reported status. This prevents false positives where
 * an experiment "succeeds" technically but causes unacceptable degradation.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0 (TDD Iteration 2)
 */
@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ExperimentRepository experimentRepository;
    private final SloEvaluator sloEvaluator;

    /**
     * Thread-safe in-memory state store for active runs
     * Maps runId -> current state information
     *
     * In production, this would be replaced with:
     * - Redis for distributed state
     * - Database for persistence
     * - Event store for audit trail
     */
    private final ConcurrentHashMap<String, RunStateInfo> runStateStore;

    /**
     * Constructor-based dependency injection
     *
     * @param experimentRepository Repository for experiment and run persistence
     * @param sloEvaluator Service for SLO evaluation and breach detection
     */
    public OrchestratorServiceImpl(
            ExperimentRepository experimentRepository,
            SloEvaluator sloEvaluator) {
        this.experimentRepository = experimentRepository;
        this.sloEvaluator = sloEvaluator;
        this.runStateStore = new ConcurrentHashMap<>();
    }

    /**
     * Dispatches a run plan for execution
     *
     * TDD Test Coverage:
     * - testDispatch_ValidRunPlan_ReturnsRunId (GREEN)
     *
     * Process:
     * 1. Persist run plan to repository
     * 2. Initialize state tracking
     * 3. Dispatch to agent (simulated for now)
     * 4. Return run ID for tracking
     *
     * AOP Integration:
     * - @ValidateSlo annotation triggers SloValidationAspect
     * - BEFORE_EXECUTION phase validates baseline SLOs
     * - Prevents experiments from running on already degraded systems
     * - Throws SloBreachException if baseline SLOs are breached
     *
     * This is a perfect example of cross-cutting concerns:
     * - Business logic focuses on orchestration
     * - AOP aspect handles SLO validation
     * - Clean separation of concerns
     *
     * @param plan The run plan containing experiment definition and scheduling
     * @return Run ID for tracking execution
     * @throws com.example.cep.aop.aspects.SloValidationAspect.SloBreachException
     *         if baseline SLOs are breached
     */
    @Override
    @ValidateSlo(
        phase = ValidationPhase.BEFORE_EXECUTION,
        abortOnBreach = true,
        breachMessage = "Cannot start experiment - baseline SLOs are already breached",
        logResults = true
    )
    public String dispatch(RunPlan plan) {
        String runId = plan.getRunId();

        // Step 1: Persist run plan
        experimentRepository.saveRunPlan(plan);
        System.out.println("Dispatched run plan: " + runId + " for experiment: " +
                         plan.getDefinition().getName());

        // Step 2: Initialize state tracking
        RunStateInfo stateInfo = new RunStateInfo(
            RunState.SCHEDULED,
            Instant.now(),
            plan.getDefinition().getName()
        );
        runStateStore.put(runId, stateInfo);

        // Step 3: Dispatch to agent - Execute fault_agent.py with ProcessBuilder
        try {
            executeFaultAgent(plan);
            System.out.println("Run " + runId + " dispatched to agent for cluster: " +
                             plan.getDefinition().getTarget().getCluster());
        } catch (Exception e) {
            System.err.println("Failed to dispatch run " + runId + ": " + e.getMessage());
            // Update state to FAILED if dispatch fails
            runStateStore.get(runId).setState(RunState.FAILED);
            throw new RuntimeException("Fault agent dispatch failed: " + e.getMessage(), e);
        }

        // Step 4: Return run ID
        return runId;
    }

    /**
     * Handles status updates from agents during experiment execution
     *
     * TDD Test Coverage:
     * - testHandleAgentUpdate_ValidUpdate_StoresStatus (GREEN)
     *
     * Agents send updates during key phases:
     * - PREPARING: Setting up fault injection
     * - INJECTING: Applying the fault
     * - OBSERVING: Monitoring effects
     * - RECOVERING: Cleaning up fault
     * - COMPLETED: Finished successfully
     * - FAILED: Encountered error
     *
     * @param runId The run ID being updated
     * @param status Current execution status
     * @param payload Additional context data from agent
     */
    @Override
    public void handleAgentUpdate(String runId, String status, Map<String, Object> payload) {
        RunStateInfo currentState = runStateStore.get(runId);

        if (currentState == null) {
            System.err.println("Warning: Received update for unknown run: " + runId);
            return;
        }

        // Update state with agent status
        currentState.setLastAgentStatus(status);
        currentState.setLastUpdate(Instant.now());

        // Store payload data for debugging
        if (payload != null && !payload.isEmpty()) {
            currentState.addPayloadData(payload);
        }

        System.out.println("Agent update for run " + runId + ": " + status +
                         " (payload keys: " + (payload != null ? payload.keySet() : "none") + ")");

        // Check if this is a terminal state
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            System.out.println("Run " + runId + " reached terminal state: " + status +
                             ". Ready for finalization.");
        }
    }

    /**
     * Finalizes a run by evaluating SLOs and generating a report
     *
     * TDD Test Coverage:
     * - testFinalizeRun_SuccessOutcome_CreatesReport (GREEN)
     * - testFinalizeRun_SloBreached_MarksAsFailed (GREEN) - CRITICAL TEST
     *
     * This method implements the core safety mechanism:
     * Even if the agent reports success, we check SLOs to ensure the experiment
     * didn't cause unacceptable degradation. If SLOs are breached, we override
     * the outcome to FAILED.
     *
     * Process:
     * 1. Retrieve run plan and state
     * 2. Evaluate SLOs using Prometheus metrics
     * 3. Check for SLO breaches
     * 4. Override outcome to FAILED if breached (CRITICAL)
     * 5. Create comprehensive report
     * 6. Persist report
     * 7. Clean up state
     *
     * @param runId The run ID to finalize
     * @param outcome The outcome reported by agent
     * @return Complete execution report with SLO analysis
     */
    @Override
    public Report finalizeRun(String runId, RunState outcome) {
        // Step 1: Retrieve run information
        RunPlan plan = experimentRepository.findRunPlan(runId);
        if (plan == null) {
            throw new IllegalArgumentException("Cannot finalize: Run plan not found for " + runId);
        }

        RunStateInfo stateInfo = runStateStore.get(runId);
        Instant startedAt = (stateInfo != null) ? stateInfo.getStartedAt() : Instant.now();
        Instant endedAt = Instant.now();

        System.out.println("Finalizing run: " + runId + " with outcome: " + outcome);

        // Step 2: Evaluate SLOs
        Map<String, Object> sloResults = sloEvaluator.evaluate(plan.getDefinition().getSlos());
        System.out.println("SLO evaluation results: " + sloResults);

        // Step 3: Check for SLO breaches (CRITICAL SAFETY CHECK)
        boolean sloBreached = sloEvaluator.breaches(sloResults);
        RunState finalOutcome = outcome;

        // Step 4: Override outcome if SLO breached
        if (sloBreached) {
            System.out.println("⚠️  SLO BREACH DETECTED for run " + runId + "!");
            System.out.println("Original outcome: " + outcome + " -> Overriding to FAILED");

            // CRITICAL: Override to FAILED regardless of agent-reported outcome
            finalOutcome = RunState.FAILED;

            // Add breach flag to results for report
            sloResults.put("breach_detected", true);
            sloResults.put("breach_reason", "One or more SLO thresholds were violated during experiment execution");
            sloResults.put("original_outcome", outcome.toString());
        } else {
            System.out.println("✓ All SLOs satisfied for run " + runId);
            sloResults.put("breach_detected", false);
        }

        // Step 5: Create comprehensive report
        Report report = new Report(
            runId,
            plan.getDefinition().getName(),
            startedAt,
            endedAt,
            finalOutcome,
            sloResults  // Include all SLO metrics and breach information
        );

        // Step 6: Persist report
        experimentRepository.saveReport(report);
        System.out.println("Report generated for run " + runId + " with final outcome: " + finalOutcome);

        // Step 7: Clean up state (run is complete)
        runStateStore.remove(runId);

        return report;
    }

    /**
     * Retrieves the current state of a running experiment
     *
     * Queries the in-memory state store for real-time run status.
     * If run is not found in memory, it means either:
     * 1. Run has completed and state was cleaned up
     * 2. Run was never started
     * 3. System was restarted (state is not persistent)
     *
     * @param runId The run ID to query
     * @return Current RunState, or null if not found in active runs
     */
    @Override
    public RunState getRunState(String runId) {
        RunStateInfo stateInfo = runStateStore.get(runId);

        if (stateInfo == null) {
            // Run not found in active runs - could be completed or never started
            return null;
        }

        return stateInfo.getState();
    }

    // ==================== Fault Agent Execution ====================

    /**
     * Executes the fault_agent.py script using ProcessBuilder
     *
     * Builds command arguments from RunPlan and executes the Python script
     * that performs actual fault injection via Docker commands.
     *
     * @param plan The run plan containing experiment configuration
     * @throws Exception if script execution fails
     */
    private void executeFaultAgent(RunPlan plan) throws Exception {
        ExperimentDefinition def = plan.getDefinition();

        // Build command arguments
        String faultType = mapFaultType(def.getFaultType());
        String target = def.getTarget().getCluster(); // or namespace, depending on your needs
        String duration = String.valueOf(def.getTimeout().getSeconds());
        String mode = plan.isDryRun() ? "dry-run" : "production";
        String intensity = def.getParameters().getOrDefault("intensity", "50").toString();

        // Build the command
        ProcessBuilder processBuilder = new ProcessBuilder(
            "python3",
            "/app/backend/fault_agent.py",
            "--fault-type", faultType,
            "--target", target,
            "--duration", duration,
            "--mode", mode,
            "--intensity", intensity
        );

        // Redirect output to logs
        processBuilder.redirectErrorStream(true);

        System.out.println("Executing fault agent: " + String.join(" ", processBuilder.command()));

        // Start the process (non-blocking)
        Process process = processBuilder.start();

        // Optional: Read output in a separate thread for logging
        new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[FaultAgent] " + line);
                }
            } catch (Exception e) {
                System.err.println("Error reading fault agent output: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Maps FaultType enum to fault_agent.py fault type strings
     *
     * @param faultType The FaultType enum value
     * @return String representation for fault_agent.py
     */
    private String mapFaultType(FaultType faultType) {
        return switch (faultType) {
            case CPU_STRESS -> "cpu";
            case MEMORY_STRESS -> "memory";
            case NETWORK_DELAY -> "network";
            case NETWORK_PARTITION -> "network";
            case POD_KILL -> "exception";
        };
    }

    // ==================== Internal State Management ====================

    /**
     * Internal class for tracking run state
     * Thread-safe when used with ConcurrentHashMap
     */
    private static class RunStateInfo {
        private RunState state;
        private Instant startedAt;
        private Instant lastUpdate;
        private String experimentName;
        private String lastAgentStatus;
        private final ConcurrentHashMap<String, Object> payloadData;

        public RunStateInfo(RunState state, Instant startedAt, String experimentName) {
            this.state = state;
            this.startedAt = startedAt;
            this.lastUpdate = startedAt;
            this.experimentName = experimentName;
            this.payloadData = new ConcurrentHashMap<>();
        }

        public RunState getState() {
            return state;
        }

        public void setState(RunState state) {
            this.state = state;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public Instant getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(Instant lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public String getExperimentName() {
            return experimentName;
        }

        public String getLastAgentStatus() {
            return lastAgentStatus;
        }

        public void setLastAgentStatus(String lastAgentStatus) {
            this.lastAgentStatus = lastAgentStatus;
        }

        public void addPayloadData(Map<String, Object> data) {
            if (data != null) {
                this.payloadData.putAll(data);
            }
        }

        public Map<String, Object> getPayloadData() {
            return payloadData;
        }
    }
}

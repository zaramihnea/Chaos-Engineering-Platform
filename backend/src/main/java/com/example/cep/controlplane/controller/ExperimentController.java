package com.example.cep.controlplane.controller;

import com.example.cep.controlplane.api.ControlPlaneApi;
import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.Report;
import com.example.cep.model.RunPlan;
import com.example.cep.model.RunState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Chaos Engineering Experiment Management
 *
 * This controller exposes HTTP endpoints for creating, scheduling, and monitoring
 * chaos engineering experiments in the Control Plane.
 *
 * Endpoints:
 * - POST   /api/experiments           - Create new experiment
 * - GET    /api/experiments           - List all experiments
 * - DELETE /api/experiments/{id}      - Delete experiment
 * - POST   /api/experiments/{id}/runs - Schedule experiment run
 * - GET    /api/runs/{runId}/state    - Get run state
 * - GET    /api/runs/{runId}/report   - Get run report
 * - DELETE /api/runs/{runId}          - Abort running experiment
 * - POST   /api/experiments/{id}/approve - Approve experiment
 * - POST   /api/experiments/validate  - Validate experiment against policy
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Chaos Engineering", description = "APIs for managing chaos engineering experiments")
public class ExperimentController {

    private final ControlPlaneApi controlPlaneApi;

    /**
     * Constructor-based dependency injection
     *
     * @param controlPlaneApi The Control Plane API service
     */
    public ExperimentController(ControlPlaneApi controlPlaneApi) {
        this.controlPlaneApi = controlPlaneApi;
    }

    /**
     * Create a new chaos experiment
     *
     * POST /api/experiments
     *
     * @param definition The experiment definition
     * @return Response with experiment ID
     */
    @Operation(
        summary = "Create a new chaos experiment",
        description = "Creates a new experiment definition with fault type, target system, and SLO thresholds. The experiment is validated against organizational policies before creation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Experiment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid experiment definition or policy violation")
    })
    @PostMapping("/experiments")
    public ResponseEntity<Map<String, String>> createExperiment(
            @Parameter(description = "Experiment definition with fault type, target, and SLOs")
            @RequestBody ExperimentDefinition definition) {
        try {
            String experimentId = controlPlaneApi.createExperiment(definition);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                    "experimentId", experimentId,
                    "message", "Experiment created successfully"
                ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all experiments
     *
     * GET /api/experiments
     *
     * @return List of all experiment definitions
     */
    @Operation(
        summary = "List all experiments",
        description = "Retrieves all experiment definitions in the system"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved experiment list")
    @GetMapping("/experiments")
    public ResponseEntity<List<ExperimentDefinition>> listExperiments() {
        List<ExperimentDefinition> experiments = controlPlaneApi.listExperiments();
        return ResponseEntity.ok(experiments);
    }

    /**
     * Delete an experiment
     *
     * DELETE /api/experiments/{id}
     *
     * @param id The experiment ID
     * @return Response confirming deletion
     */
    @Operation(
        summary = "Delete an experiment",
        description = "Deletes an experiment definition from the system. This allows the experiment ID to be reused."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Experiment deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    @DeleteMapping("/experiments/{id}")
    public ResponseEntity<Map<String, String>> deleteExperiment(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable String id) {
        try {
            controlPlaneApi.deleteExperiment(id);
            return ResponseEntity.ok(Map.of(
                "experimentId", id,
                "message", "Experiment deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Schedule an experiment run
     *
     * POST /api/experiments/{id}/runs
     *
     * @param id The experiment ID
     * @param request Request body with scheduling parameters
     * @return Response with run ID
     */
    @Operation(
        summary = "Schedule an experiment run",
        description = "Schedules an experiment for immediate or delayed execution. Can be run in dry-run mode for validation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Run scheduled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or SLO baseline breach"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    @PostMapping("/experiments/{id}/runs")
    public ResponseEntity<Map<String, String>> scheduleRun(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable String id,
            @Parameter(description = "Run scheduling parameters (when, dryRun)")
            @RequestBody ScheduleRunRequest request) {
        try {
            Instant when = request.when() != null ? request.when() : Instant.now();
            boolean dryRun = request.dryRun() != null ? request.dryRun() : false;

            String runId = controlPlaneApi.scheduleRun(id, when, dryRun);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                    "runId", runId,
                    "message", "Run scheduled successfully",
                    "dryRun", String.valueOf(dryRun)
                ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get run state
     *
     * GET /api/runs/{runId}/state
     *
     * @param runId The run ID
     * @return Current run state
     */
    @GetMapping("/runs/{runId}/state")
    public ResponseEntity<?> getRunState(@PathVariable String runId) {
        try {
            RunState state = controlPlaneApi.getRunState(runId);
            return ResponseEntity.ok(Map.of(
                "runId", runId,
                "state", state.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get run report
     *
     * GET /api/runs/{runId}/report
     *
     * @param runId The run ID
     * @return Execution report
     */
    @GetMapping("/runs/{runId}/report")
    public ResponseEntity<?> getReport(@PathVariable String runId) {
        try {
            Report report = controlPlaneApi.getReport(runId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Abort a running experiment
     *
     * DELETE /api/runs/{runId}
     *
     * @param runId The run ID
     * @param request Request body with abort reason
     * @return Response confirming abortion
     */
    @DeleteMapping("/runs/{runId}")
    public ResponseEntity<Map<String, String>> abortRun(
            @PathVariable String runId,
            @RequestBody AbortRunRequest request) {
        try {
            String reason = request.reason() != null ? request.reason() : "Manual abort";
            controlPlaneApi.abortRun(runId, reason);
            return ResponseEntity.ok(Map.of(
                "runId", runId,
                "message", "Run aborted successfully",
                "reason", reason
            ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve an experiment
     *
     * POST /api/experiments/{id}/approve
     *
     * @param id The experiment ID
     * @param request Request body with approver information
     * @return Response with approval ID
     */
    @PostMapping("/experiments/{id}/approve")
    public ResponseEntity<Map<String, String>> approveExperiment(
            @PathVariable String id,
            @RequestBody ApprovalRequest request) {
        try {

            String approver = request.approver() != null ? request.approver() : "system";
            String approvalId = controlPlaneApi.approveExperiment(id, approver);
            return ResponseEntity.ok(Map.of(
                "experimentId", id,
                "approvalId", approvalId,
                "approver", approver,
                "message", "Experiment approved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate experiment against policy
     *
     * POST /api/experiments/validate
     *
     * @param definition The experiment definition to validate
     * @return Validation result
     */
    @PostMapping("/experiments/validate")
    public ResponseEntity<Map<String, Object>> validatePolicy(@RequestBody ExperimentDefinition definition) {
        boolean isValid = controlPlaneApi.validatePolicy(definition);
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "message", isValid ? "Experiment passes all policy checks" : "Experiment violates policy"
        ));
    }
    /**
     * Get all scheduled runs for an experiment
     *
     * GET /api/experiments/{id}/runs
     */
    @Operation(
            summary = "List all scheduled runs for an experiment",
            description = "Returns all RunPlan objects associated with the given experiment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Runs retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    @GetMapping("/experiments/{id}/runs")
    public ResponseEntity<?> listRunsForExperiment(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable String id) {

        try {
            List<RunPlan> runs = controlPlaneApi.getRunsForExperiment(id);

            return ResponseEntity.ok(runs);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/health
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Chaos Engineering Control Plane"
        ));
    }

    /**
     * Request record for scheduling a run
     */
    public record ScheduleRunRequest(Instant when, Boolean dryRun) {}

    /**
     * Request record for aborting a run
     */
    public record AbortRunRequest(String reason) {}

    /**
     * Request record for approving an experiment
     */
    public record ApprovalRequest(String approver) {}
}

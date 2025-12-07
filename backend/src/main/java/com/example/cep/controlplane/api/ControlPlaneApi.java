package com.example.cep.controlplane.api;
import com.example.cep.model.*;
import java.time.Instant;
import java.util.List;

public interface ControlPlaneApi {
    String createExperiment(ExperimentDefinition def);
    String scheduleRun(String experimentId, Instant when, boolean dryRun);
    void abortRun(String runId, String reason);
    Report getReport(String runId);
    RunState getRunState(String runId);
    List<ExperimentDefinition> listExperiments();
    void deleteExperiment(String experimentId);
    String approveExperiment(String experimentId, String approver);
    boolean validatePolicy(ExperimentDefinition def);
    List<RunPlan> getRunsForExperiment(String experimentId);
}
package com.example.cep.model;
import java.time.Instant;

public class RunPlan {
    private String runId;
    private ExperimentDefinition definition;
    private Instant scheduledAt;
    private boolean dryRun;

    public RunPlan(String runId, ExperimentDefinition definition, Instant scheduledAt, boolean dryRun) {
        this.runId = runId;
        this.definition = definition;
        this.scheduledAt = scheduledAt;
        this.dryRun = dryRun;
    }

    public String getRunId() { return runId; }
    public ExperimentDefinition getDefinition() { return definition; }
    public Instant getScheduledAt() { return scheduledAt; }
    public boolean isDryRun() { return dryRun; }
}
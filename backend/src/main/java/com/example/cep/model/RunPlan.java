package com.example.cep.model;
import java.time.Instant;

public class RunPlan {
    private String runId;
    private ExperimentDefinition definition;
    private Instant scheduledAt;
    private boolean dryRun;

    public RunPlan(String runId, ExperimentDefinition definition, Instant scheduledAt, boolean dryRun) {}
    public String getRunId() { return null; }
    public ExperimentDefinition getDefinition() { return null; }
    public Instant getScheduledAt() { return null; }
    public boolean isDryRun() { return false; }
}
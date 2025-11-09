package com.example.cep.model;
import java.time.Instant;
import java.util.Map;

public class Report {
    private String runId;
    private String experimentName;
    private Instant startedAt;
    private Instant endedAt;
    private RunState outcome;
    private Map<String,Object> sloDeltas;

    public Report(String runId, String experimentName, Instant startedAt, Instant endedAt,
                  RunState outcome, Map<String,Object> sloDeltas) {
        this.runId = runId;
        this.experimentName = experimentName;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.outcome = outcome;
        this.sloDeltas = sloDeltas;
    }

    public String getRunId() { return runId; }
    public String getExperimentName() { return experimentName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public RunState getOutcome() { return outcome; }
    public Map<String,Object> getSloDeltas() { return sloDeltas; }
}
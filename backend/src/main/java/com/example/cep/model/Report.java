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
                  RunState outcome, Map<String,Object> sloDeltas) {}
    public String getRunId() { return null; }
    public String getExperimentName() { return null; }
    public Instant getStartedAt() { return null; }
    public Instant getEndedAt() { return null; }
    public RunState getOutcome() { return null; }
    public Map<String,Object> getSloDeltas() { return null; }
}
package com.example.cep.model;

public class WorkflowStep {
    private String id;
    private ExperimentDefinition experiment;
    private long waitSecondsBeforeNext;

    public WorkflowStep(String id, ExperimentDefinition experiment, long waitSecondsBeforeNext) {}
    public String getId() { return null; }
    public ExperimentDefinition getExperiment() { return null; }
    public long getWaitSecondsBeforeNext() { return 0; }
}
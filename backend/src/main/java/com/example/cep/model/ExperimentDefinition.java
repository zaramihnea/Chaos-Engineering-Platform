package com.example.cep.model;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ExperimentDefinition {
    private String id;
    private String name;
    private FaultType faultType;
    private Map<String,Object> parameters;
    private TargetSystem target;
    private Duration timeout;
    private List<SloTarget> slos;
    private boolean dryRunAllowed;
    private String createdBy;

    public ExperimentDefinition(String id, String name, FaultType faultType, Map<String,Object> parameters,
                                TargetSystem target, Duration timeout, List<SloTarget> slos,
                                boolean dryRunAllowed, String createdBy) {}
    public String getId() { return null; }
    public String getName() { return null; }
    public FaultType getFaultType() { return null; }
    public Map<String,Object> getParameters() { return null; }
    public TargetSystem getTarget() { return null; }
    public Duration getTimeout() { return null; }
    public List<SloTarget> getSlos() { return null; }
    public boolean isDryRunAllowed() { return false; }
    public String getCreatedBy() { return null; }
}
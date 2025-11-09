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
                                boolean dryRunAllowed, String createdBy) {
        this.id = id;
        this.name = name;
        this.faultType = faultType;
        this.parameters = parameters;
        this.target = target;
        this.timeout = timeout;
        this.slos = slos;
        this.dryRunAllowed = dryRunAllowed;
        this.createdBy = createdBy;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public FaultType getFaultType() { return faultType; }
    public Map<String,Object> getParameters() { return parameters; }
    public TargetSystem getTarget() { return target; }
    public Duration getTimeout() { return timeout; }
    public List<SloTarget> getSlos() { return slos; }
    public boolean isDryRunAllowed() { return dryRunAllowed; }
    public String getCreatedBy() { return createdBy; }
}
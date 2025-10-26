package com.example.cep.model;
import java.util.List;

public class Workflow {
    private String id;
    private String name;
    private List<WorkflowStep> steps;
    private boolean allowParallel;

    public Workflow(String id, String name, List<WorkflowStep> steps, boolean allowParallel) {}
    public String getId() { return null; }
    public String getName() { return null; }
    public List<WorkflowStep> getSteps() { return null; }
    public boolean isAllowParallel() { return false; }
}
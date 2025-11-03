package com.example.workflowui.models;

import java.util.List;

public class Workflow {
    private String id;
    private String name;
    private String description;
    private List<WorkflowStep> steps;
    private List<Dependency> dependencies;

    public void addSteps(WorkflowStep step) {}
    public void configureParameters(Parameter parameter) {}
    public void defineDependencies(Dependency dependency) {}
    public void run() {}
    public void abort() {}
}


package com.example.workflowui.models;

public class Dependency {
    private WorkflowStep sourceStep;
    private WorkflowStep targetStep;
    private String condition;

    public void validate() {}
}

package com.example.workflowui;

import com.example.workflowui.models.Workflow;

import java.util.List;

/**
 * Singleton Pattern
 * One instance of WorkflowManager exists in the entire system,
 * providing global access point to manage workflows.
 */
public class WorkflowManager {
    private static WorkflowManager instance;
    private List<Workflow> workflows;

    private WorkflowManager() {
        System.out.println("WorkflowManager instance created.");
    }

    public static WorkflowManager getInstance() {
        if (instance == null) {
            instance = new WorkflowManager();
        }
        return instance;
    }

    public void registerWorkflow(Workflow workflow) {}
    public void executeWorkflow(Workflow workflow) {
        System.out.println("Workflow is executing...");
    }
    public void abortWorkflow(Workflow workflow) {}
}

package com.example.workflowui;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer Pattern
 * One to many dependency between objects: The subject (MonitorExecutionOfWorkflow) tracks the workflow state.
 * When the state changes the Observer (RealTimeStatus) is notified.
 */
public class MonitorExecutionWorkflow implements WorkflowSubject {
    private String workflowId;
    private String currentState;

    public MonitorExecutionWorkflow() {
        List<WorkflowObserver> observers = new ArrayList<>();
    }

    @Override
    public void attach(WorkflowObserver observer) {}

    @Override
    public void detach(WorkflowObserver observer) {}

    @Override
    public void notifyObservers(String newState) {}

    public void changeState(String newState) {}
}


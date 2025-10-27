package com.example.workflowui;

public interface WorkflowSubject {
    void attach(WorkflowObserver observer);
    void detach(WorkflowObserver observer);
    void notifyObservers(String status);
}


package com.example.workflowui.models;

import com.example.workflowui.WorkflowObserver;

public class RealTimeStatus implements WorkflowObserver {
    private String currentState;
    private String lastEventTimestamp;
    private String details;

    public void startTracking() {}
    public void stopTracking() {}
    public void refresh() {}

    @Override
    public void update(String newState) {}
}

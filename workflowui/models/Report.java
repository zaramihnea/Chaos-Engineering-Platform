package com.example.workflowui.models;

public abstract class Report {
    protected String workflowId;
    protected String generatedAt;
    protected String results;

    public abstract void export();
}


package com.example.workflowui;

import com.example.workflowui.models.JSONReport;
import com.example.workflowui.models.MarkdownReport;
import com.example.workflowui.models.Report;

/**
 * Factory Method Pattern
 * The ReportFactory decides which class to instantiate -> JSONReport or MarkDownReport
 */
public class ReportFactory {
    public static Report createReport(String type) {
        if (type.equalsIgnoreCase("JSON")) {
            return new JSONReport();
        } else if (type.equalsIgnoreCase("MARKDOWN")) {
            return new MarkdownReport();
        }
        return null;
    }
}


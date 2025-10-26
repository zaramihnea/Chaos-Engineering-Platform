package com.example.cep.controlplane.store;
import com.example.cep.model.*;
import java.util.List;

public interface ExperimentRepository {
    void saveDefinition(ExperimentDefinition def);
    ExperimentDefinition findById(String id);
    List<ExperimentDefinition> findAll();
    void saveRunPlan(RunPlan plan);
    RunPlan findRunPlan(String runId);
    void saveReport(Report report);
    Report findReport(String runId);
}
package com.example.cep.controlplane.store;

import com.example.cep.model.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stub implementation of ExperimentRepository
 * This is a temporary implementation for testing the Spring context
 * In production, this would be replaced with actual database persistence
 */
@Repository
public class InMemoryExperimentRepository implements ExperimentRepository {

    private final Map<String, ExperimentDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, RunPlan> runPlans = new ConcurrentHashMap<>();
    private final Map<String, Report> reports = new ConcurrentHashMap<>();

    @Override
    public void saveDefinition(ExperimentDefinition def) {
        definitions.put(def.getId(), def);
    }

    @Override
    public ExperimentDefinition findById(String id) {
        return definitions.get(id);
    }

    @Override
    public List<ExperimentDefinition> findAll() {
        return new ArrayList<>(definitions.values());
    }

    @Override
    public void deleteById(String id) {
        definitions.remove(id);
    }

    @Override
    public void saveRunPlan(RunPlan plan) {
        runPlans.put(plan.getRunId(), plan);
    }

    @Override
    public RunPlan findRunPlan(String runId) {
        return runPlans.get(runId);
    }

    @Override
    public void saveReport(Report report) {
        reports.put(report.getRunId(), report);
    }

    @Override
    public Report findReport(String runId) {
        return reports.get(runId);
    }

    @Override
    public List<RunPlan> findRunsByExperimentId(String experimentId) {
        return runPlans.values().stream()
                .filter(r -> r.getDefinition().getId().equals(experimentId))
                .toList();
    }
}

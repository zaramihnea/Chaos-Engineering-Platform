package com.example.cep.controlplane.service;
import com.example.cep.model.*;

public interface OrchestratorService {
    String dispatch(RunPlan plan);
    void handleAgentUpdate(String runId, String status, java.util.Map<String,Object> payload);
    Report finalizeRun(String runId, RunState outcome);
}
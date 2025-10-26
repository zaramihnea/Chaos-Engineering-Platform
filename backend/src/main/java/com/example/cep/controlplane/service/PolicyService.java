package com.example.cep.controlplane.service;
import com.example.cep.model.ExperimentDefinition;

public interface PolicyService {
    boolean isAllowed(ExperimentDefinition def);
    String denialReason(ExperimentDefinition def);
}
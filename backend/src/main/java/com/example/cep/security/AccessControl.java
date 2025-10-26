package com.example.cep.security;
import com.example.cep.model.ExperimentDefinition;

public interface AccessControl {
    boolean canCreate(AuthContext ctx, ExperimentDefinition def);
    boolean canRun(AuthContext ctx, String experimentId);
    boolean canApprove(AuthContext ctx, String experimentId);
}
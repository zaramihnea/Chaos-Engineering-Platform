package com.example.cep.controlplane.service;
import com.example.cep.model.SloTarget;
import java.util.List;
import java.util.Map;

public interface SloEvaluator {
    Map<String,Object> evaluate(List<SloTarget> slos);
    boolean breaches(Map<String,Object> results);
}
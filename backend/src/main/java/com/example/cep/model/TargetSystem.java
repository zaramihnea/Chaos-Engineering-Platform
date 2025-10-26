package com.example.cep.model;
import java.util.Map;
public class TargetSystem {
    private String cluster;
    private String namespace;
    private Map<String,String> labels;
    public TargetSystem(String cluster, String namespace, Map<String,String> labels) {}
    public String getCluster() { return null; }
    public String getNamespace() { return null; }
    public Map<String,String> getLabels() { return null; }
}
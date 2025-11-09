package com.example.cep.model;
import java.util.Map;
public class TargetSystem {
    private String cluster;
    private String namespace;
    private Map<String,String> labels;

    public TargetSystem(String cluster, String namespace, Map<String,String> labels) {
        this.cluster = cluster;
        this.namespace = namespace;
        this.labels = labels;
    }

    public String getCluster() { return cluster; }
    public String getNamespace() { return namespace; }
    public Map<String,String> getLabels() { return labels; }
}
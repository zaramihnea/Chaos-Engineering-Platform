package com.example.cep.model;
public class SloTarget {
    private SloMetric metric;
    private String promQuery;
    private double threshold;
    private String comparator;
    public SloTarget(SloMetric metric, String promQuery, double threshold, String comparator) {}
    public SloMetric getMetric() { return null; }
    public String getPromQuery() { return null; }
    public double getThreshold() { return 0; }
    public String getComparator() { return null; }
}
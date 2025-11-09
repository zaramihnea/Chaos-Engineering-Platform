package com.example.cep.model;
public class SloTarget {
    private SloMetric metric;
    private String promQuery;
    private double threshold;
    private String comparator;

    public SloTarget(SloMetric metric, String promQuery, double threshold, String comparator) {
        this.metric = metric;
        this.promQuery = promQuery;
        this.threshold = threshold;
        this.comparator = comparator;
    }

    public SloMetric getMetric() { return metric; }
    public String getPromQuery() { return promQuery; }
    public double getThreshold() { return threshold; }
    public String getComparator() { return comparator; }
}
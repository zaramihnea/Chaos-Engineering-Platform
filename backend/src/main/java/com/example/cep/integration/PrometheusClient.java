package com.example.cep.integration;
import java.util.Map;

public interface PrometheusClient {
    Map<String,Object> queryRange(String promQl, long startEpochSec, long endEpochSec, long stepSec);
    Map<String,Object> queryInstant(String promQl);
}
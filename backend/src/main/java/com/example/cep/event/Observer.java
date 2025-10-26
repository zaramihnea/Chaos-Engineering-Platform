package com.example.cep.event;
import java.util.Map;

public interface Observer {
    void update(String event, Map<String,Object> data);
}
package com.example.cep.event;
import java.util.Map;

public interface Subject {
    void attach(Observer o);
    void detach(Observer o);
    void notifyObservers(String event, Map<String,Object> data);
}
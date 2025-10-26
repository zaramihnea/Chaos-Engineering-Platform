package com.example.cep.event;
import java.util.List;
import java.util.Map;

public class RunEventBus implements Subject {
    private List<Observer> observers;
    @Override public void attach(Observer o) {}
    @Override public void detach(Observer o) {}
    @Override public void notifyObservers(String event, Map<String,Object> data) {}
}
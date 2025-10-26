package com.example.cep.aop;

public aspect PolicyAspect {
    pointcut scheduleRun(): execution(* com.example.cep.controlplane.api.ControlPlaneApi.scheduleRun(..));
    before(): scheduleRun() {}
}
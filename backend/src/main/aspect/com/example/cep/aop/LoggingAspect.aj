package com.example.cep.aop;

public aspect LoggingAspect {
    pointcut lifecycle(): execution(* com.example.cep.controlplane.service.OrchestratorService.finalizeRun(..));
    before(): lifecycle() {}
    after(): lifecycle() {}
}
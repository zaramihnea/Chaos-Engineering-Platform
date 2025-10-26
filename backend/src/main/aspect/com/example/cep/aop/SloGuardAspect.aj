package com.example.cep.aop;

public aspect SloGuardAspect {
    pointcut sloDecision(): execution(* com.example.cep.controlplane.service.SloEvaluator.breaches(..));
    after(): sloDecision() {}
}
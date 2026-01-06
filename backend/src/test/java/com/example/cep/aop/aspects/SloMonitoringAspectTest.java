package com.example.cep.aop.aspects;

import com.example.cep.aop.annotations.MonitorSlo;
import com.example.cep.controlplane.service.SloEvaluator;
import com.example.cep.model.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SLO Monitoring Aspect Tests")
class SloMonitoringAspectTest {

    @Mock
    private SloEvaluator mockSloEvaluator;

    @Mock
    private ProceedingJoinPoint mockJoinPoint;

    @Mock
    private MethodSignature mockSignature;

    @Mock
    private Method mockMethod;

    @Mock
    private MonitorSlo mockMonitorSlo;

    private SloMonitoringAspect aspect;
    private RunPlan testRunPlan;
    private ExperimentDefinition testDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new SloMonitoringAspect(mockSloEvaluator);

        TargetSystem target = new TargetSystem("test-cluster", "default", Map.of());
        SloTarget slo = new SloTarget(SloMetric.LATENCY_P95, "http_latency", 500.0, "<");

        testDefinition = new ExperimentDefinition(
            "exp-1", "Test Exp",
            FaultType.POD_KILL,
            Map.of(),
            target,
            Duration.ofMinutes(5),
            List.of(slo),
            false,
            "test-user"
        );

        testRunPlan = new RunPlan("run-1", testDefinition, Instant.now(), false);
    }

    @Test
    @DisplayName("constructor initializes monitoring executor")
    void testConstructor() {
        assertNotNull(aspect);
    }

    @Test
    @DisplayName("monitoring proceeds when no SLO targets found")
    void testNoSloTargets() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{"no-slo-data"});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("testMethod");
        when(mockMonitorSlo.monitoringId()).thenReturn("");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(5);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(300);
        when(mockJoinPoint.proceed()).thenReturn("result");

        Object result = aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);

        assertEquals("result", result);
        verify(mockJoinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("monitoring executes with RunPlan argument")
    void testMonitoringWithRunPlan() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testRunPlan});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("executeExperiment");
        when(mockMonitorSlo.monitoringId()).thenReturn("test-123");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(100);
            return "completed";
        });
        when(mockSloEvaluator.evaluate(any())).thenReturn(Map.of("latency_p95", 250.0));
        when(mockSloEvaluator.breaches(any())).thenReturn(false);

        Object result = aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);

        assertEquals("completed", result);
        verify(mockJoinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("monitoring executes with ExperimentDefinition argument")
    void testMonitoringWithExperimentDefinition() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testDefinition});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("createExperiment");
        when(mockMonitorSlo.monitoringId()).thenReturn("");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenReturn("created");
        when(mockSloEvaluator.evaluate(any())).thenReturn(Map.of());
        when(mockSloEvaluator.breaches(any())).thenReturn(false);

        Object result = aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);

        assertEquals("created", result);
    }

    @Test
    @DisplayName("monitoring detects SLO breach")
    void testMonitoringDetectsBreach() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testRunPlan});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("executeExperiment");
        when(mockMonitorSlo.monitoringId()).thenReturn("breach-test");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(true);
        when(mockMonitorSlo.alertOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(150);
            return "result";
        });

        Map<String, Object> breachResults = Map.of(
            "latency_p95", 750.0,
            "latency_p95_threshold", 500.0,
            "latency_p95_comparator", "<"
        );
        when(mockSloEvaluator.evaluate(any())).thenReturn(breachResults);
        when(mockSloEvaluator.breaches(any())).thenReturn(true);

        assertThrows(SloMonitoringAspect.SloBreachDuringExecutionException.class, () -> {
            aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);
        });
    }

    @Test
    @DisplayName("monitoring continues when abortOnBreach is false")
    void testMonitoringContinuesWithoutAbort() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testRunPlan});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("executeExperiment");
        when(mockMonitorSlo.monitoringId()).thenReturn("no-abort");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(100);
            return "result";
        });

        when(mockSloEvaluator.evaluate(any())).thenReturn(Map.of("latency_p95", 750.0));
        when(mockSloEvaluator.breaches(any())).thenReturn(true);

        Object result = aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);

        assertEquals("result", result);
    }

    @Test
    @DisplayName("monitoring handles method exception")
    void testMonitoringHandlesException() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testRunPlan});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("executeExperiment");
        when(mockMonitorSlo.monitoringId()).thenReturn("exception-test");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenThrow(new RuntimeException("Method failed"));

        assertThrows(RuntimeException.class, () -> {
            aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);
        });
    }

    @Test
    @DisplayName("SloBreachDuringExecutionException stores results")
    void testSloBreachException() {
        Map<String, Object> results = Map.of("latency", 1000.0);
        SloMonitoringAspect.SloBreachDuringExecutionException ex =
            new SloMonitoringAspect.SloBreachDuringExecutionException("Test breach", results);

        assertEquals("Test breach", ex.getMessage());
        assertEquals(results, ex.getSloResults());
    }

    @Test
    @DisplayName("destroy shuts down executor")
    void testDestroy() {
        assertDoesNotThrow(() -> aspect.destroy());
    }

    @Test
    @DisplayName("monitoring with custom monitoring ID")
    void testCustomMonitoringId() throws Throwable {
        when(mockJoinPoint.getArgs()).thenReturn(new Object[]{testRunPlan});
        when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getName()).thenReturn("test");
        when(mockMonitorSlo.monitoringId()).thenReturn("custom-id-123");
        when(mockMonitorSlo.intervalSeconds()).thenReturn(1);
        when(mockMonitorSlo.maxDurationSeconds()).thenReturn(5);
        when(mockMonitorSlo.abortOnBreach()).thenReturn(false);
        when(mockJoinPoint.proceed()).thenReturn("done");
        when(mockSloEvaluator.evaluate(any())).thenReturn(Map.of());
        when(mockSloEvaluator.breaches(any())).thenReturn(false);

        Object result = aspect.monitorSlosDuringExecution(mockJoinPoint, mockMonitorSlo);

        assertEquals("done", result);
    }
}

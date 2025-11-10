# Aspect-Oriented Programming (AOP) for SLO Validation

## Overview

This package implements **Aspect-Oriented Programming (AOP)** to handle the cross-cutting concern of **Service Level Objective (SLO) validation** across the Chaos Engineering Platform.

### Why AOP for SLO Validation?

SLO validation is a **cross-cutting concern** that needs to be applied consistently across multiple components:
- Control Plane API
- Orchestrator Service
- Workflow Engine
- Policy Service

Without AOP, we would need to:
- ❌ Duplicate SLO validation code in every service method
- ❌ Mix business logic with validation logic
- ❌ Risk inconsistent validation behavior
- ❌ Make code harder to maintain and test

With AOP, we can:
- ✅ Centralize SLO validation logic in aspects
- ✅ Apply validation declaratively via annotations
- ✅ Keep business logic clean and focused
- ✅ Ensure consistent validation across all components

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    AOP SLO Validation Architecture              │
└─────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │   Annotations       │
                    │  - @ValidateSlo     │
                    │  - @MonitorSlo      │
                    └──────────┬──────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                         Aspects                                  │
│  ┌─────────────────────┐      ┌─────────────────────────┐      │
│  │ SloValidationAspect │      │ SloMonitoringAspect     │      │
│  │                     │      │                         │      │
│  │ - Before Advice     │      │ - Continuous Monitoring │      │
│  │ - After Advice      │      │ - Background Thread     │      │
│  │ - Around Advice     │      │ - Real-time Abort       │      │
│  └─────────┬───────────┘      └───────────┬─────────────┘      │
└────────────┼────────────────────────────────┼────────────────────┘
             │                                │
             └────────────┬───────────────────┘
                          ▼
                 ┌────────────────────┐
                 │ SloViolationHandler│
                 │                    │
                 │ - Record violations│
                 │ - Analyze severity │
                 │ - Recommend actions│
                 └──────────┬─────────┘
                            │
                            ▼
                 ┌────────────────────┐
                 │   SloEvaluator     │
                 │                    │
                 │ - Query Prometheus │
                 │ - Check thresholds │
                 └────────────────────┘
```

---

## Components

### 1. Annotations

#### `@ValidateSlo`
Marks methods that require SLO validation at specific lifecycle points.

```java
@ValidateSlo(
    phase = ValidationPhase.BEFORE_EXECUTION,
    abortOnBreach = true,
    breachMessage = "Cannot start experiment - baseline SLOs breached",
    logResults = true
)
public String dispatch(RunPlan plan) {
    // Business logic here
}
```

**Parameters:**
- `phase` - When to validate: BEFORE_EXECUTION, AFTER_EXECUTION, AROUND_EXECUTION
- `abortOnBreach` - Whether to abort operation on SLO breach (default: true)
- `breachMessage` - Custom error message (default: generic message)
- `logResults` - Whether to log SLO results (default: true)

#### `@MonitorSlo`
Enables continuous SLO monitoring for long-running operations.

```java
@MonitorSlo(
    intervalSeconds = 10,
    maxDurationSeconds = 300,
    abortOnBreach = true,
    alertOnBreach = true
)
public void executeExperiment(RunPlan plan) {
    // Long-running chaos experiment
}
```

**Parameters:**
- `intervalSeconds` - How often to check SLOs (default: 15s)
- `maxDurationSeconds` - Maximum monitoring duration (default: 600s)
- `abortOnBreach` - Whether to abort on breach (default: true)
- `alertOnBreach` - Whether to send alerts on breach (default: true)

---

### 2. Aspects

#### `SloValidationAspect`
Intercepts methods annotated with `@ValidateSlo` and validates SLOs at specified lifecycle points.

**Advice Types:**
- `@Before` - Validates before method execution (baseline check)
- `@AfterReturning` - Validates after successful execution (recovery check)
- `@Around` - Wraps execution with before + after validation

**Example Flow:**
```
Client Code
    │
    ├─> @ValidateSlo(BEFORE_EXECUTION)
    │       │
    │       ├─> SloValidationAspect intercepts
    │       ├─> Query Prometheus
    │       ├─> Check thresholds
    │       ├─> If breach → throw SloBreachException
    │       └─> If pass → proceed
    │
    ├─> Business Logic Executes
    │
    └─> Return Result
```

#### `SloMonitoringAspect`
Wraps methods annotated with `@MonitorSlo` and spawns background thread for continuous monitoring.

**How it works:**
1. Method is called
2. Aspect extracts SLO targets from arguments
3. Spawns background monitoring thread
4. Thread queries Prometheus every N seconds
5. If breach detected, sets abort flag
6. Main method execution continues
7. After method completes, thread stops
8. If abort flag is set, throw exception

---

### 3. SloViolationHandler

Centralized service for handling all SLO violations.

**Features:**
- Records violation history
- Analyzes severity (LOW, MEDIUM, HIGH, CRITICAL)
- Determines if experiment should abort
- Recommends remediation actions
- Generates statistics

**Usage:**
```java
@Autowired
private SloViolationHandler violationHandler;

// Record a violation
ViolationResponse response = violationHandler.recordViolation(
    experimentId,
    ViolationType.BASELINE_BREACH,
    sloResults
);

// Check if should abort
boolean shouldAbort = violationHandler.shouldAbortExperiment(experimentId);

// Get statistics
Map<String, Object> stats = violationHandler.getViolationStatistics(experimentId);
```

---

## Usage Examples

### Example 1: Baseline Validation Before Experiment

```java
@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    @Override
    @ValidateSlo(
        phase = ValidationPhase.BEFORE_EXECUTION,
        abortOnBreach = true
    )
    public String dispatch(RunPlan plan) {
        // This method will only execute if baseline SLOs are met
        // Aspect validates SLOs automatically before this code runs

        experimentRepository.saveRunPlan(plan);
        return plan.getRunId();
    }
}
```

**What happens:**
1. Client calls `dispatch(plan)`
2. **BEFORE** method runs, aspect intercepts
3. Aspect queries Prometheus for current metrics
4. If latency > 500ms or error rate > 1%, **abort**
5. If SLOs OK, proceed with dispatch logic

---

### Example 2: Full Validation (Before + After)

```java
@ValidateSlo(
    phase = ValidationPhase.AROUND_EXECUTION,
    abortOnBreach = true,
    logResults = true
)
public Report executeAndValidate(RunPlan plan) {
    // BEFORE: Aspect validates baseline SLOs

    // Execute chaos experiment
    injectFault(plan);
    Thread.sleep(Duration.ofMinutes(2));
    removeFault(plan);

    // AFTER: Aspect validates recovery SLOs

    return generateReport();
}
```

**What happens:**
1. **BEFORE**: Check baseline SLOs
2. If breach → abort
3. Execute experiment
4. **AFTER**: Check recovery SLOs
5. If recovery failed → throw exception
6. Return report

---

### Example 3: Continuous Monitoring

```java
@MonitorSlo(
    intervalSeconds = 10,
    maxDurationSeconds = 600,
    abortOnBreach = true
)
public void runLongExperiment(RunPlan plan) {
    // Background thread monitors SLOs every 10 seconds

    for (int i = 0; i < 10; i++) {
        injectFault(i);
        Thread.sleep(Duration.ofMinutes(1));

        // If SLO breach detected, aspect sets abort flag
        // On next iteration or method exit, exception thrown
    }
}
```

**What happens:**
1. Method starts
2. Aspect spawns monitoring thread
3. Thread queries Prometheus every 10s
4. If breach at T+45s, abort flag set
5. Method completes
6. Aspect checks abort flag → throws exception

---

## Testing

### Unit Testing Aspects

Aspects require Spring context to work (AOP is container-managed).

```java
@SpringBootTest
class SloValidationAspectTest {

    @Autowired
    private TestService testService;  // Spring-managed bean

    @MockBean
    private SloEvaluator sloEvaluator;

    @Test
    void shouldAbortWhenBaselineBreached() {
        // Arrange
        when(sloEvaluator.breaches(any())).thenReturn(true);

        // Act & Assert
        assertThrows(SloBreachException.class, () -> {
            testService.methodWithValidation(plan);
        });
    }
}
```

---

## Configuration

### Enable AOP

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    // Enables Spring AOP support
}
```

### Maven Dependencies

```xml
<!-- Spring AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- AspectJ -->
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
```

---

## Benefits Demonstrated

### 1. **Separation of Concerns**
- Business logic: Focus on orchestration, fault injection, reporting
- Validation logic: Centralized in aspects
- Clear, maintainable code

### 2. **DRY Principle**
- Single aspect validates SLOs for all experiments
- No code duplication across services

### 3. **Consistency**
- All experiments validated the same way
- No risk of forgetting validation

### 4. **Testability**
- Aspects can be tested independently
- Business logic can be tested without validation overhead
- Mock SloEvaluator to control test scenarios

### 5. **Flexibility**
- Easy to add validation to new methods (just add annotation)
- Easy to change validation logic (modify aspect)
- Easy to disable validation (remove annotation)

---

## SOA/Enterprise Architecture Principles

### Service Abstraction
- SLO validation abstracted behind annotations
- Callers don't need to know validation implementation

### Service Reusability
- Single aspect reused across all chaos operations
- SloViolationHandler reused by multiple aspects

### Service Loose Coupling
- Aspects depend only on SloEvaluator interface
- Business services don't depend on validation logic

### Service Autonomy
- Aspects independently manage validation lifecycle
- No coordination needed with business logic

### Policy Centralization
- All validation rules in one place
- Easy to update policies globally

---

## Future Enhancements

1. **Integration with Alerting Systems**
   - PagerDuty, Slack, email notifications
   - Triggered when SLO breaches occur

2. **Advanced Monitoring**
   - ML-based anomaly detection
   - Predictive SLO breach alerts

3. **Auto-Remediation**
   - Trigger automated rollback on breach
   - Scale resources to restore SLOs

4. **Compliance Auditing**
   - Record all SLO validations
   - Generate compliance reports

---

## Author
**Zară Mihnea-Tudor**
Scrum Master & Backend Developer
Chaos Engineering Platform

---

## References
- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [AspectJ Programming Guide](https://www.eclipse.org/aspectj/doc/released/progguide/)
- [Chaos Engineering Principles](https://principlesofchaos.org/)

# TDD Iteration Documentation

```bash
mvn test
```

## Iteration 1: Experiment Creation & Policy Validation

- Services
    - `backend/src/main/java/com/example/cep/controlplane/api/ControlPlaneApiImpl.java`
    - `backend/src/main/java/com/example/cep/controlplane/service/PolicyServiceImpl.java`
- Tests
    - `backend/src/test/java/com/example/cep/controlplane/api/ControlPlaneApiTest.java`:
        - `testCreateExperiment_ValidDefinition_ReturnsId` – verifies valid experiment creation returns experiment ID.
        - `testCreateExperiment_PolicyViolation_ThrowsException` – verifies policy violations throw PolicyViolationException.
        - `testValidatePolicy_AllowedExperiment_ReturnsTrue` – verifies allowed experiments return true.
        - `testValidatePolicy_DisallowedExperiment_ReturnsFalse` – verifies disallowed experiments return false.
        - `testListExperiments_ReturnsAllExperiments` – verifies listing returns all stored experiments.

## Iteration 2: Orchestration & SLO Evaluation

- Services
    - `backend/src/main/java/com/example/cep/controlplane/service/OrchestratorServiceImpl.java`
    - `backend/src/main/java/com/example/cep/controlplane/service/SloEvaluatorImpl.java`
- Tests
    - `backend/src/test/java/com/example/cep/controlplane/service/OrchestratorServiceTest.java`:
        - `testDispatch_ValidRunPlan_ReturnsRunId` – verifies dispatch returns run ID and initializes state.
        - `testHandleAgentUpdate_ValidUpdate_StoresStatus` – verifies agent updates are processed correctly.
        - `testFinalizeRun_SuccessOutcome_CreatesReport` – verifies successful runs create complete reports.
        - `testFinalizeRun_SloBreached_MarksAsFailed` – verifies SLO breaches override outcome to FAILED.
    - `backend/src/test/java/com/example/cep/controlplane/service/SloEvaluatorTest.java`:
        - `testEvaluate_ValidSlos_ReturnsMetrics` – verifies SLO evaluation returns metrics from Prometheus.
        - `testBreaches_ThresholdExceeded_ReturnsTrue` – verifies breach detection when thresholds exceeded.
        - `testBreaches_WithinThreshold_ReturnsFalse` – verifies no breach when metrics within thresholds.

package com.example.cep.controlplane.store;

import com.example.cep.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for InMemoryExperimentRepository
 */
@DisplayName("InMemoryExperimentRepository Tests")
class InMemoryExperimentRepositoryTest {

    private InMemoryExperimentRepository repository;
    private ExperimentDefinition testDefinition;
    private RunPlan testRunPlan;
    private Report testReport;

    @BeforeEach
    void setUp() {
        repository = new InMemoryExperimentRepository();

        // Create test data
        TargetSystem target = new TargetSystem("test-cluster", "default", Map.of("app", "test"));
        SloTarget slo = new SloTarget(SloMetric.LATENCY_P95, "http_request_duration_seconds", 200.0, "<");

        testDefinition = new ExperimentDefinition(
                "exp-1",
                "Test Experiment",
                FaultType.POD_KILL,
                Map.of("severity", "low"),
                target,
                java.time.Duration.ofMinutes(5),
                List.of(slo),
                false,
                "test-user"
        );

        testRunPlan = new RunPlan("run-1", testDefinition, Instant.now(), false);
        testReport = new Report("run-1", "Test Experiment", Instant.now(), Instant.now(),
                RunState.COMPLETED, Map.of("latency_p95", 150.0));
    }

    @Test
    @DisplayName("saveDefinition and findById")
    void testSaveAndFindDefinition() {
        repository.saveDefinition(testDefinition);

        ExperimentDefinition found = repository.findById("exp-1");

        assertNotNull(found);
        assertEquals("exp-1", found.getId());
        assertEquals("Test Experiment", found.getName());
    }

    @Test
    @DisplayName("findById returns null for non-existent id")
    void testFindByIdNotFound() {
        ExperimentDefinition found = repository.findById("non-existent");

        assertNull(found);
    }

    @Test
    @DisplayName("findAll returns all definitions")
    void testFindAll() {
        ExperimentDefinition def1 = new ExperimentDefinition(
                "exp-1", "Test 1",
                FaultType.POD_KILL,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(5),
                List.of(),
                false,
                "user1"
        );
        ExperimentDefinition def2 = new ExperimentDefinition(
                "exp-2", "Test 2",
                FaultType.NETWORK_DELAY,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(5),
                List.of(),
                false,
                "user2"
        );

        repository.saveDefinition(def1);
        repository.saveDefinition(def2);

        List<ExperimentDefinition> all = repository.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(d -> d.getId().equals("exp-1")));
        assertTrue(all.stream().anyMatch(d -> d.getId().equals("exp-2")));
    }

    @Test
    @DisplayName("findAll returns empty list when no definitions")
    void testFindAllEmpty() {
        List<ExperimentDefinition> all = repository.findAll();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("deleteById removes definition")
    void testDeleteById() {
        repository.saveDefinition(testDefinition);
        assertEquals(1, repository.findAll().size());

        repository.deleteById("exp-1");

        assertNull(repository.findById("exp-1"));
        assertEquals(0, repository.findAll().size());
    }

    @Test
    @DisplayName("deleteById on non-existent id does not throw")
    void testDeleteByIdNotFound() {
        assertDoesNotThrow(() -> repository.deleteById("non-existent"));
    }

    @Test
    @DisplayName("saveRunPlan and findRunPlan")
    void testSaveAndFindRunPlan() {
        repository.saveRunPlan(testRunPlan);

        RunPlan found = repository.findRunPlan("run-1");

        assertNotNull(found);
        assertEquals("run-1", found.getRunId());
        assertFalse(found.isDryRun());
    }

    @Test
    @DisplayName("findRunPlan returns null for non-existent runId")
    void testFindRunPlanNotFound() {
        RunPlan found = repository.findRunPlan("non-existent");

        assertNull(found);
    }

    @Test
    @DisplayName("saveReport and findReport")
    void testSaveAndFindReport() {
        repository.saveReport(testReport);

        Report found = repository.findReport("run-1");

        assertNotNull(found);
        assertEquals("run-1", found.getRunId());
        assertEquals(RunState.COMPLETED, found.getOutcome());
    }

    @Test
    @DisplayName("findReport returns null for non-existent runId")
    void testFindReportNotFound() {
        Report found = repository.findReport("non-existent");

        assertNull(found);
    }

    @Test
    @DisplayName("findRunsByExperimentId returns matching runs")
    void testFindRunsByExperimentId() {
        ExperimentDefinition exp1 = new ExperimentDefinition(
                "exp-1", "Test 1",
                FaultType.POD_KILL,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(5),
                List.of(),
                false,
                "user1"
        );
        ExperimentDefinition exp2 = new ExperimentDefinition(
                "exp-2", "Test 2",
                FaultType.NETWORK_DELAY,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(5),
                List.of(),
                false,
                "user2"
        );

        RunPlan run1 = new RunPlan("run-1", exp1, Instant.now(), false);
        RunPlan run2 = new RunPlan("run-2", exp1, Instant.now(), false);
        RunPlan run3 = new RunPlan("run-3", exp2, Instant.now(), false);

        repository.saveRunPlan(run1);
        repository.saveRunPlan(run2);
        repository.saveRunPlan(run3);

        List<RunPlan> exp1Runs = repository.findRunsByExperimentId("exp-1");

        assertEquals(2, exp1Runs.size());
        assertTrue(exp1Runs.stream().anyMatch(r -> r.getRunId().equals("run-1")));
        assertTrue(exp1Runs.stream().anyMatch(r -> r.getRunId().equals("run-2")));
        assertFalse(exp1Runs.stream().anyMatch(r -> r.getRunId().equals("run-3")));
    }

    @Test
    @DisplayName("findRunsByExperimentId returns empty list for non-existent experiment")
    void testFindRunsByExperimentIdNotFound() {
        List<RunPlan> runs = repository.findRunsByExperimentId("non-existent");

        assertNotNull(runs);
        assertTrue(runs.isEmpty());
    }

    @Test
    @DisplayName("saveDefinition overwrites existing definition with same id")
    void testSaveDefinitionOverwrite() {
        repository.saveDefinition(testDefinition);

        ExperimentDefinition updated = new ExperimentDefinition(
                "exp-1", "Updated Description",
                FaultType.NETWORK_DELAY,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(10),
                List.of(),
                true,
                "user-updated"
        );
        repository.saveDefinition(updated);

        ExperimentDefinition found = repository.findById("exp-1");
        assertEquals("Updated Description", found.getName());
        assertEquals(FaultType.NETWORK_DELAY, found.getFaultType());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("saveRunPlan overwrites existing run plan with same runId")
    void testSaveRunPlanOverwrite() {
        repository.saveRunPlan(testRunPlan);

        RunPlan updated = new RunPlan("run-1", testDefinition, Instant.now(), true);
        repository.saveRunPlan(updated);

        RunPlan found = repository.findRunPlan("run-1");
        assertTrue(found.isDryRun());
    }

    @Test
    @DisplayName("saveReport overwrites existing report with same runId")
    void testSaveReportOverwrite() {
        repository.saveReport(testReport);

        Report updated = new Report("run-1", "Test Experiment", Instant.now(), Instant.now(),
                RunState.FAILED, Map.of());
        repository.saveReport(updated);

        Report found = repository.findReport("run-1");
        assertEquals(RunState.FAILED, found.getOutcome());
    }

    @Test
    @DisplayName("repository handles multiple concurrent saves")
    void testConcurrentSaves() {
        for (int i = 0; i < 10; i++) {
            ExperimentDefinition def = new ExperimentDefinition(
                    "exp-" + i, "Test " + i,
                    FaultType.POD_KILL,
                    Map.of(),
                    new TargetSystem("cluster", "ns", Map.of()),
                    java.time.Duration.ofMinutes(5),
                    List.of(),
                    false,
                    "user" + i
            );
            repository.saveDefinition(def);
        }

        assertEquals(10, repository.findAll().size());
    }

    @Test
    @DisplayName("findRunsByExperimentId filters correctly")
    void testFindRunsByExperimentIdFiltering() {
        ExperimentDefinition exp1 = new ExperimentDefinition(
                "exp-1", "Test",
                FaultType.POD_KILL,
                Map.of(),
                new TargetSystem("cluster", "ns", Map.of()),
                java.time.Duration.ofMinutes(5),
                List.of(),
                false,
                "user1"
        );

        repository.saveRunPlan(new RunPlan("run-1", exp1, Instant.now(), false));
        repository.saveRunPlan(new RunPlan("run-2", exp1, Instant.now(), false));
        repository.saveRunPlan(new RunPlan("run-3", exp1, Instant.now(), false));

        List<RunPlan> runs = repository.findRunsByExperimentId("exp-1");

        assertEquals(3, runs.size());
    }

    @Test
    @DisplayName("InMemoryExperimentRepository implements ExperimentRepository")
    void testImplementsInterface() {
        assertTrue(repository instanceof ExperimentRepository);
    }
}

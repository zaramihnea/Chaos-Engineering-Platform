package com.example.cep.aop.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling SLO violations and orchestrating automated responses
 *
 * This service acts as a centralized handler for all SLO breach events detected
 * by AOP aspects. It provides:
 * - Violation recording and history tracking
 * - Automated abort decision making
 * - Notification and alerting
 * - Remediation action triggering
 * - Compliance and audit logging
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * DESIGN RATIONALE
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Why separate handler service?
 * 1. Separation of Concerns: Aspects focus on interception, handler focuses on response
 * 2. Testability: Can unit test violation handling logic independently
 * 3. Reusability: Multiple aspects (validation, monitoring) can share same handler
 * 4. Flexibility: Easy to add new violation response strategies
 *
 * SOA Principles:
 * - Service Abstraction: Hides complexity of violation handling
 * - Service Reusability: Used by multiple AOP aspects
 * - Service Autonomy: Independently manages violation state and responses
 * - Service Composability: Can integrate with external alerting/remediation systems
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * INTEGRATION WITH AOP ASPECTS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Workflow:
 * ┌──────────────────┐       ┌────────────────────┐       ┌──────────────────┐
 * │ SloValidation    │──────>│ SloViolation       │──────>│ External Systems │
 * │ Aspect           │       │ Handler (this)     │       │ (Alerts, etc.)   │
 * └──────────────────┘       └────────────────────┘       └──────────────────┘
 * ┌──────────────────┐              │
 * │ SloMonitoring    │──────────────┘
 * │ Aspect           │
 * └──────────────────┘
 *
 * Both aspects delegate violation handling to this service, ensuring consistent
 * response behavior across all SLO validation mechanisms.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Service
public class SloViolationHandler {

    // Thread-safe storage for violation history
    private final ConcurrentHashMap<String, List<ViolationRecord>> violationHistory;

    // Configuration for violation thresholds
    private final int maxViolationsBeforeAbort;
    private final long violationWindowMillis;

    /**
     * Constructor with default configuration
     */
    public SloViolationHandler() {
        this.violationHistory = new ConcurrentHashMap<>();
        this.maxViolationsBeforeAbort = 3;  // Abort after 3 violations
        this.violationWindowMillis = 60000; // Within 60 second window
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Record an SLO violation event
     *
     * This method is called by AOP aspects when an SLO breach is detected.
     * It records the violation, analyzes severity, and determines if abort is needed.
     *
     * @param experimentId Unique identifier for the experiment
     * @param violationType Type of violation (BASELINE_BREACH, RUNTIME_BREACH, RECOVERY_FAILURE)
     * @param sloResults Detailed SLO evaluation results
     * @return ViolationResponse containing recommended actions
     */
    public ViolationResponse recordViolation(
        String experimentId,
        ViolationType violationType,
        Map<String, Object> sloResults
    ) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ SLO VIOLATION HANDLER                                        ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Experiment:    " + String.format("%-45s", experimentId) + " ║");
        System.out.println("║ Violation:     " + String.format("%-45s", violationType) + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // Create violation record
        ViolationRecord record = new ViolationRecord(
            UUID.randomUUID().toString(),
            experimentId,
            violationType,
            Instant.now(),
            sloResults
        );

        // Store in history
        violationHistory.computeIfAbsent(experimentId, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(record);

        System.out.println("📝 Violation recorded: " + record.getId());

        // Analyze severity and determine response
        ViolationResponse response = analyzeAndRespond(experimentId, record);

        System.out.println("📊 Response: " + response);
        System.out.println();

        return response;
    }

    /**
     * Check if experiment should be aborted based on violation history
     *
     * @param experimentId Experiment to check
     * @return true if experiment should abort
     */
    public boolean shouldAbortExperiment(String experimentId) {
        List<ViolationRecord> records = violationHistory.get(experimentId);
        if (records == null || records.isEmpty()) {
            return false;
        }

        // Get recent violations within time window
        long cutoffTime = System.currentTimeMillis() - violationWindowMillis;
        long recentViolations = records.stream()
            .filter(r -> r.getTimestamp().toEpochMilli() > cutoffTime)
            .count();

        return recentViolations >= maxViolationsBeforeAbort;
    }

    /**
     * Get violation history for an experiment
     *
     * @param experimentId Experiment ID
     * @return List of violation records
     */
    public List<ViolationRecord> getViolationHistory(String experimentId) {
        return new ArrayList<>(violationHistory.getOrDefault(experimentId, Collections.emptyList()));
    }

    /**
     * Get violation statistics for an experiment
     *
     * @param experimentId Experiment ID
     * @return Map of violation statistics
     */
    public Map<String, Object> getViolationStatistics(String experimentId) {
        List<ViolationRecord> records = violationHistory.getOrDefault(experimentId, Collections.emptyList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_violations", records.size());
        stats.put("baseline_breaches", countByType(records, ViolationType.BASELINE_BREACH));
        stats.put("runtime_breaches", countByType(records, ViolationType.RUNTIME_BREACH));
        stats.put("recovery_failures", countByType(records, ViolationType.RECOVERY_FAILURE));

        if (!records.isEmpty()) {
            stats.put("first_violation", records.get(0).getTimestamp());
            stats.put("last_violation", records.get(records.size() - 1).getTimestamp());
        }

        return stats;
    }

    /**
     * Clear violation history for an experiment
     * (Usually called when experiment completes)
     *
     * @param experimentId Experiment ID
     */
    public void clearViolationHistory(String experimentId) {
        violationHistory.remove(experimentId);
        System.out.println("🗑️  Cleared violation history for experiment: " + experimentId);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Analyze violation and determine appropriate response
     */
    private ViolationResponse analyzeAndRespond(String experimentId, ViolationRecord record) {
        ViolationResponse response = new ViolationResponse();

        // Determine severity based on violation type
        Severity severity = determineSeverity(record.getType(), record.getSloResults());
        response.setSeverity(severity);

        // Check if abort is recommended
        boolean shouldAbort = shouldAbortExperiment(experimentId);
        response.setShouldAbort(shouldAbort);

        // Determine if alerts should be sent
        boolean shouldAlert = severity == Severity.CRITICAL || severity == Severity.HIGH;
        response.setShouldAlert(shouldAlert);

        // Generate recommended actions
        List<String> actions = generateRecommendedActions(record, shouldAbort);
        response.setRecommendedActions(actions);

        return response;
    }

    /**
     * Determine severity level of violation
     */
    private Severity determineSeverity(ViolationType type, Map<String, Object> sloResults) {
        // Baseline breaches are always critical (system not healthy before experiment)
        if (type == ViolationType.BASELINE_BREACH) {
            return Severity.CRITICAL;
        }

        // Recovery failures are high severity
        if (type == ViolationType.RECOVERY_FAILURE) {
            return Severity.HIGH;
        }

        // For runtime breaches, analyze the magnitude
        // (Simplified - in production, would analyze actual vs threshold delta)
        return Severity.MEDIUM;
    }

    /**
     * Generate list of recommended actions based on violation
     */
    private List<String> generateRecommendedActions(ViolationRecord record, boolean shouldAbort) {
        List<String> actions = new ArrayList<>();

        if (shouldAbort) {
            actions.add("ABORT_EXPERIMENT");
            actions.add("NOTIFY_ON_CALL_ENGINEER");
        }

        switch (record.getType()) {
            case BASELINE_BREACH:
                actions.add("INVESTIGATE_SYSTEM_HEALTH");
                actions.add("DELAY_EXPERIMENT");
                break;

            case RUNTIME_BREACH:
                actions.add("INCREASE_MONITORING_FREQUENCY");
                actions.add("PREPARE_ROLLBACK");
                break;

            case RECOVERY_FAILURE:
                actions.add("TRIGGER_REMEDIATION");
                actions.add("SCALE_UP_RESOURCES");
                actions.add("CREATE_INCIDENT_TICKET");
                break;
        }

        return actions;
    }

    /**
     * Count violations by type
     */
    private long countByType(List<ViolationRecord> records, ViolationType type) {
        return records.stream()
            .filter(r -> r.getType() == type)
            .count();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Types of SLO violations
     */
    public enum ViolationType {
        /**
         * SLO breach detected before experiment started (baseline unhealthy)
         */
        BASELINE_BREACH,

        /**
         * SLO breach detected during experiment execution
         */
        RUNTIME_BREACH,

        /**
         * System failed to recover to acceptable SLO levels after experiment
         */
        RECOVERY_FAILURE
    }

    /**
     * Severity levels for violations
     */
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Record of a single violation event
     */
    public static class ViolationRecord {
        private final String id;
        private final String experimentId;
        private final ViolationType type;
        private final Instant timestamp;
        private final Map<String, Object> sloResults;

        public ViolationRecord(
            String id,
            String experimentId,
            ViolationType type,
            Instant timestamp,
            Map<String, Object> sloResults
        ) {
            this.id = id;
            this.experimentId = experimentId;
            this.type = type;
            this.timestamp = timestamp;
            this.sloResults = new HashMap<>(sloResults);
        }

        public String getId() { return id; }
        public String getExperimentId() { return experimentId; }
        public ViolationType getType() { return type; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getSloResults() { return new HashMap<>(sloResults); }
    }

    /**
     * Response object containing recommended actions for a violation
     */
    public static class ViolationResponse {
        private Severity severity;
        private boolean shouldAbort;
        private boolean shouldAlert;
        private List<String> recommendedActions;

        public ViolationResponse() {
            this.recommendedActions = new ArrayList<>();
        }

        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }

        public boolean isShouldAbort() { return shouldAbort; }
        public void setShouldAbort(boolean shouldAbort) { this.shouldAbort = shouldAbort; }

        public boolean isShouldAlert() { return shouldAlert; }
        public void setShouldAlert(boolean shouldAlert) { this.shouldAlert = shouldAlert; }

        public List<String> getRecommendedActions() { return new ArrayList<>(recommendedActions); }
        public void setRecommendedActions(List<String> actions) { this.recommendedActions = new ArrayList<>(actions); }

        @Override
        public String toString() {
            return "ViolationResponse{" +
                   "severity=" + severity +
                   ", shouldAbort=" + shouldAbort +
                   ", shouldAlert=" + shouldAlert +
                   ", actions=" + recommendedActions +
                   '}';
        }
    }
}

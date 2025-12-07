package com.example.cep.mop.service;

import com.example.cep.mop.model.PolicyState;
import com.example.cep.model.SloTarget;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing policy state snapshots and detecting policy changes
 *
 * This service is the central component for MOP-based policy monitoring.
 * It maintains snapshots of SLO policies and provides verification methods
 * to detect policy drift during operation execution.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MOP ARCHITECTURE - STATE MANAGEMENT COMPONENT
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * MOP System Components:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                                                                             │
 * │  @MonitorPolicyCompliance  ──────> PolicyValidationAspect                  │
 * │      (Annotation)                        (Interceptor)                     │
 * │                                               │                             │
 * │                                               ▼                             │
 * │                                    PolicyStateService                      │
 * │                                      (State Manager)                       │
 * │                                               │                             │
 * │                           ┌──────────────────┼──────────────────┐          │
 * │                           ▼                  ▼                  ▼          │
 * │                    captureSnapshot()   validatePolicy()   recordViolation()│
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Responsibilities:
 * 1. OBSERVATION: Capture policy snapshots at operation start
 * 2. VERIFICATION: Compare current policy against snapshot
 * 3. TRACKING: Maintain history of policy changes and violations
 * 4. REPORTING: Provide metrics and audit data
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. SNAPSHOT PATTERN:
 *    - Captures immutable policy state at point in time
 *    - Enables comparison against current state
 *    - Provides rollback reference if needed
 *
 * 2. MEMENTO PATTERN:
 *    - PolicyState acts as memento (state snapshot)
 *    - Service acts as caretaker (manages mementos)
 *    - Enables temporal reasoning about policy evolution
 *
 * 3. THREAD-SAFETY:
 *    - ConcurrentHashMap for multi-threaded access
 *    - Immutable PolicyState snapshots
 *    - Atomic operations for validation
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
@Service
public class PolicyStateService {

    /**
     * Storage for active policy snapshots
     * Key: experimentId
     * Value: PolicyState snapshot
     */
    private final ConcurrentHashMap<String, PolicyState> policySnapshots;

    /**
     * History of policy violations
     * Key: experimentId
     * Value: List of violation records
     */
    private final ConcurrentHashMap<String, List<PolicyViolation>> violationHistory;

    /**
     * Metrics tracking
     */
    private final ConcurrentHashMap<String, PolicyMetrics> metricsMap;

    /**
     * Constructor
     */
    public PolicyStateService() {
        this.policySnapshots = new ConcurrentHashMap<>();
        this.violationHistory = new ConcurrentHashMap<>();
        this.metricsMap = new ConcurrentHashMap<>();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // OBSERVATION - Capture Policy Snapshots
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Capture a snapshot of the current policy state
     *
     * This is called at the start of an operation to record the policy
     * that should remain consistent throughout execution.
     *
     * @param experimentId Unique identifier for the operation
     * @param sloTargets Current SLO policy targets
     * @return PolicyState snapshot
     */
    public PolicyState captureSnapshot(String experimentId, List<SloTarget> sloTargets) {
        PolicyState snapshot = new PolicyState(experimentId, sloTargets);
        policySnapshots.put(experimentId, snapshot);

        // Initialize metrics
        metricsMap.put(experimentId, new PolicyMetrics(experimentId));

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ MOP: Policy Snapshot Captured                                ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Experiment:  " + String.format("%-47s", experimentId) + " ║");
        System.out.println("║ Snapshot ID: " + String.format("%-47s", snapshot.getSnapshotId().substring(0, 8)) + " ║");
        System.out.println("║ SLO Count:   " + String.format("%-47s", sloTargets.size()) + " ║");
        System.out.println("║ Hash:        " + String.format("%-47s", snapshot.getPolicyHash()) + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        return snapshot;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // VERIFICATION - Validate Policy Consistency
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Validate that the current policy matches the captured snapshot
     *
     * This is the core MOP verification method. It compares the current policy
     * against the snapshot to detect policy drift.
     *
     * @param experimentId Experiment to validate
     * @param currentSloTargets Current SLO policy
     * @return ValidationResult containing match status and details
     */
    public ValidationResult validatePolicy(String experimentId, List<SloTarget> currentSloTargets) {
        PolicyState snapshot = policySnapshots.get(experimentId);

        if (snapshot == null) {
            return ValidationResult.noSnapshot(experimentId);
        }

        // Create current state for comparison
        PolicyState currentState = new PolicyState(experimentId, currentSloTargets);

        // Update metrics
        PolicyMetrics metrics = metricsMap.get(experimentId);
        if (metrics != null) {
            metrics.incrementValidationCount();
        }

        // Perform validation
        boolean matches = snapshot.matches(currentState);

        if (matches) {
            System.out.println("✅ MOP Validation: Policy unchanged (experiment: " + experimentId + ")");
            return ValidationResult.valid(experimentId, snapshot, currentState);
        } else {
            // Policy has changed - this is a violation
            List<String> differences = snapshot.getDifferences(currentState);

            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║ ❌ MOP VIOLATION: Policy Changed During Execution            ║");
            System.out.println("╠════════════════════════════════════════════════════════════════╣");
            System.out.println("║ Experiment:  " + String.format("%-47s", experimentId) + " ║");
            System.out.println("║ Differences: " + String.format("%-47s", differences.size()) + " ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");

            for (String diff : differences) {
                System.out.println("   - " + diff);
            }

            // Record violation
            PolicyViolation violation = new PolicyViolation(
                experimentId,
                snapshot,
                currentState,
                differences,
                Instant.now()
            );

            violationHistory.computeIfAbsent(experimentId, k -> Collections.synchronizedList(new ArrayList<>()))
                           .add(violation);

            if (metrics != null) {
                metrics.incrementViolationCount();
            }

            return ValidationResult.invalid(experimentId, snapshot, currentState, differences);
        }
    }

    /**
     * Quick check if policy has changed
     *
     * @param experimentId Experiment to check
     * @param currentSloTargets Current policy
     * @return true if policy changed
     */
    public boolean hasPolicyChanged(String experimentId, List<SloTarget> currentSloTargets) {
        ValidationResult result = validatePolicy(experimentId, currentSloTargets);
        return !result.isValid();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TRACKING & REPORTING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get policy snapshot for an experiment
     *
     * @param experimentId Experiment ID
     * @return PolicyState snapshot, or null if not found
     */
    public PolicyState getSnapshot(String experimentId) {
        return policySnapshots.get(experimentId);
    }

    /**
     * Get violation history for an experiment
     *
     * @param experimentId Experiment ID
     * @return List of violations
     */
    public List<PolicyViolation> getViolationHistory(String experimentId) {
        return new ArrayList<>(violationHistory.getOrDefault(experimentId, Collections.emptyList()));
    }

    /**
     * Get metrics for an experiment
     *
     * @param experimentId Experiment ID
     * @return Policy metrics
     */
    public PolicyMetrics getMetrics(String experimentId) {
        return metricsMap.get(experimentId);
    }

    /**
     * Clear snapshot and history for completed experiment
     *
     * @param experimentId Experiment ID
     */
    public void clearExperiment(String experimentId) {
        policySnapshots.remove(experimentId);
        violationHistory.remove(experimentId);
        metricsMap.remove(experimentId);

        System.out.println("🗑️  MOP: Cleared policy state for experiment: " + experimentId);
    }

    /**
     * Get all active experiments being monitored
     *
     * @return Set of experiment IDs
     */
    public Set<String> getActiveExperiments() {
        return new HashSet<>(policySnapshots.keySet());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Result of policy validation
     */
    public static class ValidationResult {
        private final String experimentId;
        private final boolean valid;
        private final PolicyState originalPolicy;
        private final PolicyState currentPolicy;
        private final List<String> differences;
        private final String message;

        private ValidationResult(
            String experimentId,
            boolean valid,
            PolicyState originalPolicy,
            PolicyState currentPolicy,
            List<String> differences,
            String message
        ) {
            this.experimentId = experimentId;
            this.valid = valid;
            this.originalPolicy = originalPolicy;
            this.currentPolicy = currentPolicy;
            this.differences = differences != null ? new ArrayList<>(differences) : Collections.emptyList();
            this.message = message;
        }

        public static ValidationResult valid(String experimentId, PolicyState original, PolicyState current) {
            return new ValidationResult(experimentId, true, original, current, null, "Policy unchanged");
        }

        public static ValidationResult invalid(
            String experimentId,
            PolicyState original,
            PolicyState current,
            List<String> differences
        ) {
            return new ValidationResult(
                experimentId,
                false,
                original,
                current,
                differences,
                "Policy changed during execution"
            );
        }

        public static ValidationResult noSnapshot(String experimentId) {
            return new ValidationResult(
                experimentId,
                false,
                null,
                null,
                Collections.singletonList("No policy snapshot found"),
                "No policy snapshot captured"
            );
        }

        public String getExperimentId() { return experimentId; }
        public boolean isValid() { return valid; }
        public PolicyState getOriginalPolicy() { return originalPolicy; }
        public PolicyState getCurrentPolicy() { return currentPolicy; }
        public List<String> getDifferences() { return new ArrayList<>(differences); }
        public String getMessage() { return message; }
    }

    /**
     * Record of a policy violation
     */
    public static class PolicyViolation {
        private final String violationId;
        private final String experimentId;
        private final PolicyState originalPolicy;
        private final PolicyState changedPolicy;
        private final List<String> differences;
        private final Instant detectedAt;

        public PolicyViolation(
            String experimentId,
            PolicyState originalPolicy,
            PolicyState changedPolicy,
            List<String> differences,
            Instant detectedAt
        ) {
            this.violationId = UUID.randomUUID().toString();
            this.experimentId = experimentId;
            this.originalPolicy = originalPolicy;
            this.changedPolicy = changedPolicy;
            this.differences = new ArrayList<>(differences);
            this.detectedAt = detectedAt;
        }

        public String getViolationId() { return violationId; }
        public String getExperimentId() { return experimentId; }
        public PolicyState getOriginalPolicy() { return originalPolicy; }
        public PolicyState getChangedPolicy() { return changedPolicy; }
        public List<String> getDifferences() { return new ArrayList<>(differences); }
        public Instant getDetectedAt() { return detectedAt; }
    }

    /**
     * Metrics for policy monitoring
     */
    public static class PolicyMetrics {
        private final String experimentId;
        private int validationCount;
        private int violationCount;
        private final Instant startedAt;

        public PolicyMetrics(String experimentId) {
            this.experimentId = experimentId;
            this.validationCount = 0;
            this.violationCount = 0;
            this.startedAt = Instant.now();
        }

        public synchronized void incrementValidationCount() {
            validationCount++;
        }

        public synchronized void incrementViolationCount() {
            violationCount++;
        }

        public String getExperimentId() { return experimentId; }
        public int getValidationCount() { return validationCount; }
        public int getViolationCount() { return violationCount; }
        public Instant getStartedAt() { return startedAt; }

        public double getViolationRate() {
            return validationCount > 0 ? (double) violationCount / validationCount : 0.0;
        }
    }
}

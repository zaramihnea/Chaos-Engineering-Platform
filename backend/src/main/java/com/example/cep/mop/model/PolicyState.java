package com.example.cep.mop.model;

import com.example.cep.model.SloTarget;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a snapshot of SLO policy state at a point in time
 *
 * This class captures the complete state of an SLO policy, enabling the MOP
 * aspect to detect if the policy changes during operation execution.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * MONITORING-ORIENTED PROGRAMMING (MOP) - POLICY SNAPSHOT
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * MOP Concept: OBSERVATION
 * - This class represents an observed state snapshot
 * - Captured at operation start
 * - Compared against current state after each operation step
 * - Enables detection of policy drift/changes
 *
 * Key Principle:
 * Operations should execute under consistent policy constraints. If the policy
 * changes mid-execution (e.g., admin tightens SLO thresholds), the operation
 * should be aborted to prevent unexpected behavior.
 *
 * Use Case Example:
 * 1. Admin starts chaos experiment with latency_p99 < 500ms SLO
 * 2. Experiment begins, policy snapshot taken
 * 3. During execution, admin changes policy to latency_p99 < 300ms
 * 4. MOP aspect detects policy change
 * 5. Experiment is aborted to prevent violation under new stricter policy
 *
 * @author Zără Mihnea-Tudor
 * @version 1.0
 */
public class PolicyState {

    /**
     * Unique identifier for the policy snapshot
     */
    private final String snapshotId;

    /**
     * Identifier of the experiment/operation this policy applies to
     */
    private final String experimentId;

    /**
     * List of SLO targets that define the policy
     */
    private final List<SloTarget> sloTargets;

    /**
     * Timestamp when this snapshot was captured
     */
    private final Instant capturedAt;

    /**
     * Hash of the policy configuration for quick comparison
     */
    private final int policyHash;

    /**
     * Constructor
     *
     * @param experimentId ID of the experiment
     * @param sloTargets List of SLO targets defining the policy
     */
    public PolicyState(String experimentId, List<SloTarget> sloTargets) {
        this.snapshotId = java.util.UUID.randomUUID().toString();
        this.experimentId = experimentId;
        this.sloTargets = new ArrayList<>(sloTargets);
        this.capturedAt = Instant.now();
        this.policyHash = computePolicyHash(sloTargets);
    }

    /**
     * Check if this policy state matches another policy state
     *
     * This is the core verification method used by the MOP aspect.
     * It compares policy snapshots to detect changes.
     *
     * @param other The current policy state to compare against
     * @return true if policies match, false if policy has changed
     */
    public boolean matches(PolicyState other) {
        if (other == null) {
            return false;
        }

        // Quick check using hash
        if (this.policyHash != other.policyHash) {
            return false;
        }

        // Deep comparison of SLO targets
        if (this.sloTargets.size() != other.sloTargets.size()) {
            return false;
        }

        for (int i = 0; i < this.sloTargets.size(); i++) {
            SloTarget thisSlo = this.sloTargets.get(i);
            SloTarget otherSlo = other.sloTargets.get(i);

            if (!sloTargetsEqual(thisSlo, otherSlo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get detailed description of policy differences
     *
     * @param other The policy to compare against
     * @return List of differences found
     */
    public List<String> getDifferences(PolicyState other) {
        List<String> differences = new ArrayList<>();

        if (other == null) {
            differences.add("Other policy is null");
            return differences;
        }

        if (this.sloTargets.size() != other.sloTargets.size()) {
            differences.add(String.format(
                "SLO count changed: %d -> %d",
                this.sloTargets.size(),
                other.sloTargets.size()
            ));
        }

        int minSize = Math.min(this.sloTargets.size(), other.sloTargets.size());
        for (int i = 0; i < minSize; i++) {
            SloTarget thisSlo = this.sloTargets.get(i);
            SloTarget otherSlo = other.sloTargets.get(i);

            if (!Objects.equals(thisSlo.getMetric(), otherSlo.getMetric())) {
                differences.add(String.format(
                    "SLO[%d] metric changed: %s -> %s",
                    i,
                    thisSlo.getMetric().name(),
                    otherSlo.getMetric().name()
                ));
            }

            if (!Objects.equals(thisSlo.getThreshold(), otherSlo.getThreshold())) {
                differences.add(String.format(
                    "SLO[%d] threshold changed: %s -> %s",
                    i,
                    thisSlo.getThreshold(),
                    otherSlo.getThreshold()
                ));
            }

            if (!Objects.equals(thisSlo.getComparator(), otherSlo.getComparator())) {
                differences.add(String.format(
                    "SLO[%d] comparator changed: %s -> %s",
                    i,
                    thisSlo.getComparator(),
                    otherSlo.getComparator()
                ));
            }
        }

        return differences;
    }

    /**
     * Compute hash of policy for quick comparison
     *
     * @param targets SLO targets to hash
     * @return Hash value
     */
    private int computePolicyHash(List<SloTarget> targets) {
        int hash = 17;
        for (SloTarget target : targets) {
            hash = 31 * hash + (target.getMetric() != null ? target.getMetric().hashCode() : 0);
            hash = 31 * hash + Double.hashCode(target.getThreshold());
            hash = 31 * hash + (target.getComparator() != null ? target.getComparator().hashCode() : 0);
        }
        return hash;
    }

    /**
     * Check if two SLO targets are equal
     *
     * @param slo1 First SLO target
     * @param slo2 Second SLO target
     * @return true if equal
     */
    private boolean sloTargetsEqual(SloTarget slo1, SloTarget slo2) {
        return Objects.equals(slo1.getMetric(), slo2.getMetric()) &&
               Double.compare(slo1.getThreshold(), slo2.getThreshold()) == 0 &&
               Objects.equals(slo1.getComparator(), slo2.getComparator());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public List<SloTarget> getSloTargets() {
        return new ArrayList<>(sloTargets);
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public int getPolicyHash() {
        return policyHash;
    }

    @Override
    public String toString() {
        return String.format(
            "PolicyState{snapshot='%s', experiment='%s', slos=%d, hash=%d, captured=%s}",
            snapshotId.substring(0, 8),
            experimentId,
            sloTargets.size(),
            policyHash,
            capturedAt
        );
    }
}

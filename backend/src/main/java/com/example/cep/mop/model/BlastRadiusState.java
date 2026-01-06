package com.example.cep.mop.model;

import java.time.Instant;
import java.util.*;

/**
 * Represents the state of affected resources during a chaos experiment
 *
 * This class tracks the "blast radius" - the scope of impact of a fault injection.
 * It maintains real-time information about which resources are affected by the experiment.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
public class BlastRadiusState {

    private final String experimentId;
    private final Instant captureTime;
    private final Set<String> affectedPods;
    private final Set<String> affectedNamespaces;
    private final Set<String> affectedServices;
    private final Map<String, Object> metadata;

    /**
     * Creates a new blast radius state snapshot
     *
     * @param experimentId Unique identifier for the experiment
     */
    public BlastRadiusState(String experimentId) {
        this.experimentId = experimentId;
        this.captureTime = Instant.now();
        this.affectedPods = new HashSet<>();
        this.affectedNamespaces = new HashSet<>();
        this.affectedServices = new HashSet<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Copy constructor for creating snapshots
     */
    public BlastRadiusState(BlastRadiusState other) {
        this.experimentId = other.experimentId;
        this.captureTime = Instant.now();
        this.affectedPods = new HashSet<>(other.affectedPods);
        this.affectedNamespaces = new HashSet<>(other.affectedNamespaces);
        this.affectedServices = new HashSet<>(other.affectedServices);
        this.metadata = new HashMap<>(other.metadata);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Resource Tracking Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    public void addAffectedPod(String podName) {
        affectedPods.add(podName);
    }

    public void addAffectedNamespace(String namespace) {
        affectedNamespaces.add(namespace);
    }

    public void addAffectedService(String serviceName) {
        affectedServices.add(serviceName);
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Blast Radius Metrics
    // ═══════════════════════════════════════════════════════════════════════════════

    public int getAffectedPodCount() {
        return affectedPods.size();
    }

    public int getAffectedNamespaceCount() {
        return affectedNamespaces.size();
    }

    public int getAffectedServiceCount() {
        return affectedServices.size();
    }

    public boolean hasAffectedResources() {
        return !affectedPods.isEmpty() || !affectedNamespaces.isEmpty() || !affectedServices.isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Threshold Checking
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if blast radius exceeds specified thresholds
     *
     * @param maxPods Maximum allowed affected pods
     * @param maxNamespaces Maximum allowed affected namespaces
     * @param maxServices Maximum allowed affected services
     * @return true if any threshold is exceeded
     */
    public boolean exceedsThresholds(int maxPods, int maxNamespaces, int maxServices) {
        return getAffectedPodCount() > maxPods
            || getAffectedNamespaceCount() > maxNamespaces
            || getAffectedServiceCount() > maxServices;
    }

    /**
     * Get details about which thresholds are breached
     *
     * @param maxPods Maximum allowed pods
     * @param maxNamespaces Maximum allowed namespaces
     * @param maxServices Maximum allowed services
     * @return List of breach descriptions
     */
    public List<String> getBreaches(int maxPods, int maxNamespaces, int maxServices) {
        List<String> breaches = new ArrayList<>();

        if (getAffectedPodCount() > maxPods) {
            breaches.add(String.format("Pods: %d > %d (limit)",
                getAffectedPodCount(), maxPods));
        }

        if (getAffectedNamespaceCount() > maxNamespaces) {
            breaches.add(String.format("Namespaces: %d > %d (limit)",
                getAffectedNamespaceCount(), maxNamespaces));
        }

        if (getAffectedServiceCount() > maxServices) {
            breaches.add(String.format("Services: %d > %d (limit)",
                getAffectedServiceCount(), maxServices));
        }

        return breaches;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════════════════════════════════════════

    public String getExperimentId() {
        return experimentId;
    }

    public Instant getCaptureTime() {
        return captureTime;
    }

    public Set<String> getAffectedPods() {
        return Collections.unmodifiableSet(affectedPods);
    }

    public Set<String> getAffectedNamespaces() {
        return Collections.unmodifiableSet(affectedNamespaces);
    }

    public Set<String> getAffectedServices() {
        return Collections.unmodifiableSet(affectedServices);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // String Representation
    // ═══════════════════════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return String.format(
            "BlastRadius[experiment=%s, pods=%d, namespaces=%d, services=%d, time=%s]",
            experimentId,
            getAffectedPodCount(),
            getAffectedNamespaceCount(),
            getAffectedServiceCount(),
            captureTime
        );
    }

    /**
     * Get detailed summary of blast radius
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║ Blast Radius Summary                                         ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Experiment:   %-47s ║\n", experimentId));
        sb.append(String.format("║ Capture Time: %-47s ║\n", captureTime));
        sb.append("╠════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Affected Pods:       %-39d ║\n", getAffectedPodCount()));
        sb.append(String.format("║ Affected Namespaces: %-39d ║\n", getAffectedNamespaceCount()));
        sb.append(String.format("║ Affected Services:   %-39d ║\n", getAffectedServiceCount()));
        sb.append("╚════════════════════════════════════════════════════════════════╝\n");

        if (hasAffectedResources()) {
            if (!affectedPods.isEmpty()) {
                sb.append("\nAffected Pods:\n");
                affectedPods.forEach(pod -> sb.append("  - ").append(pod).append("\n"));
            }

            if (!affectedNamespaces.isEmpty()) {
                sb.append("\nAffected Namespaces:\n");
                affectedNamespaces.forEach(ns -> sb.append("  - ").append(ns).append("\n"));
            }

            if (!affectedServices.isEmpty()) {
                sb.append("\nAffected Services:\n");
                affectedServices.forEach(svc -> sb.append("  - ").append(svc).append("\n"));
            }
        }

        return sb.toString();
    }
}

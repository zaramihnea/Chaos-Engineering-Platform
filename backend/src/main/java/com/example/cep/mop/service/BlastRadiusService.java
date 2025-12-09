package com.example.cep.mop.service;

import com.example.cep.mop.model.BlastRadiusState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking and managing blast radius state during chaos experiments
 *
 * This service maintains the current state of affected resources and provides
 * methods to query and validate blast radius against safety thresholds.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Service
public class BlastRadiusService {

    // Thread-safe storage of blast radius state per experiment
    private final Map<String, BlastRadiusState> experimentStates = new ConcurrentHashMap<>();

    // History of breaches for audit trail
    private final Map<String, List<BlastRadiusBreach>> breachHistory = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════════
    // State Management
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Initialize blast radius tracking for an experiment
     *
     * @param experimentId Unique identifier for the experiment
     * @return Initial blast radius state (empty)
     */
    public BlastRadiusState initializeTracking(String experimentId) {
        BlastRadiusState state = new BlastRadiusState(experimentId);
        experimentStates.put(experimentId, state);
        breachHistory.put(experimentId, new ArrayList<>());

        System.out.println("📊 Initialized blast radius tracking for: " + experimentId);
        return state;
    }

    /**
     * Get current blast radius state for an experiment
     *
     * @param experimentId Experiment identifier
     * @return Current blast radius state, or null if not found
     */
    public BlastRadiusState getCurrentState(String experimentId) {
        return experimentStates.get(experimentId);
    }

    /**
     * Clear tracking state for an experiment
     *
     * @param experimentId Experiment identifier
     */
    public void clearExperiment(String experimentId) {
        experimentStates.remove(experimentId);
        breachHistory.remove(experimentId);
        System.out.println("🧹 Cleared blast radius tracking for: " + experimentId);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Resource Tracking
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Record that a pod has been affected by the experiment
     *
     * @param experimentId Experiment identifier
     * @param podName Name of the affected pod
     * @param namespace Namespace of the pod
     */
    public void recordAffectedPod(String experimentId, String podName, String namespace) {
        BlastRadiusState state = experimentStates.get(experimentId);
        if (state != null) {
            state.addAffectedPod(podName);
            state.addAffectedNamespace(namespace);
            System.out.println("  📍 Recorded affected pod: " + podName + " (namespace: " + namespace + ")");
        }
    }

    /**
     * Record that a service has been affected
     *
     * @param experimentId Experiment identifier
     * @param serviceName Name of the affected service
     */
    public void recordAffectedService(String experimentId, String serviceName) {
        BlastRadiusState state = experimentStates.get(experimentId);
        if (state != null) {
            state.addAffectedService(serviceName);
            System.out.println("  📍 Recorded affected service: " + serviceName);
        }
    }

    /**
     * Simulate discovering affected resources (for demo purposes)
     *
     * In a real implementation, this would query Kubernetes API to find
     * which resources are actually affected by the fault injection.
     *
     * @param experimentId Experiment identifier
     * @param simulateSpread Whether to simulate fault spreading
     */
    public void simulateResourceDiscovery(String experimentId, boolean simulateSpread) {
        BlastRadiusState state = experimentStates.get(experimentId);
        if (state == null) return;

        // Simulate discovering affected pods
        if (simulateSpread) {
            // Simulate fault spreading to multiple pods (BREACH scenario)
            recordAffectedPod(experimentId, "cart-service-pod-1", "default");
            recordAffectedPod(experimentId, "cart-service-pod-2", "default");
            recordAffectedPod(experimentId, "cart-service-pod-3", "default");
            recordAffectedPod(experimentId, "cart-service-pod-4", "default");
            recordAffectedService(experimentId, "cart-service");
        } else {
            // Normal scenario - only intended target affected
            recordAffectedPod(experimentId, "cart-service-pod-1", "default");
            recordAffectedService(experimentId, "cart-service");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Blast Radius Validation
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Validate blast radius against safety thresholds
     *
     * @param experimentId Experiment identifier
     * @param maxPods Maximum allowed affected pods
     * @param maxNamespaces Maximum allowed affected namespaces
     * @param maxServices Maximum allowed affected services
     * @return Validation result
     */
    public ValidationResult validateBlastRadius(
        String experimentId,
        int maxPods,
        int maxNamespaces,
        int maxServices
    ) {
        BlastRadiusState state = experimentStates.get(experimentId);
        if (state == null) {
            return new ValidationResult(true, Collections.emptyList());
        }

        boolean isValid = !state.exceedsThresholds(maxPods, maxNamespaces, maxServices);
        List<String> breaches = state.getBreaches(maxPods, maxNamespaces, maxServices);

        if (!isValid) {
            // Record breach in history
            BlastRadiusBreach breach = new BlastRadiusBreach(
                experimentId,
                Instant.now(),
                state.getAffectedPodCount(),
                state.getAffectedNamespaceCount(),
                state.getAffectedServiceCount(),
                breaches
            );
            breachHistory.get(experimentId).add(breach);
        }

        return new ValidationResult(isValid, breaches);
    }

    /**
     * Get breach history for an experiment
     *
     * @param experimentId Experiment identifier
     * @return List of recorded breaches
     */
    public List<BlastRadiusBreach> getBreachHistory(String experimentId) {
        return breachHistory.getOrDefault(experimentId, Collections.emptyList());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Nested Classes
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Result of blast radius validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> breaches;

        public ValidationResult(boolean valid, List<String> breaches) {
            this.valid = valid;
            this.breaches = breaches;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getBreaches() {
            return breaches;
        }
    }

    /**
     * Record of a blast radius breach
     */
    public static class BlastRadiusBreach {
        private final String experimentId;
        private final Instant timestamp;
        private final int affectedPods;
        private final int affectedNamespaces;
        private final int affectedServices;
        private final List<String> breachDetails;

        public BlastRadiusBreach(
            String experimentId,
            Instant timestamp,
            int affectedPods,
            int affectedNamespaces,
            int affectedServices,
            List<String> breachDetails
        ) {
            this.experimentId = experimentId;
            this.timestamp = timestamp;
            this.affectedPods = affectedPods;
            this.affectedNamespaces = affectedNamespaces;
            this.affectedServices = affectedServices;
            this.breachDetails = breachDetails;
        }

        public String getExperimentId() {
            return experimentId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getAffectedPods() {
            return affectedPods;
        }

        public int getAffectedNamespaces() {
            return affectedNamespaces;
        }

        public int getAffectedServices() {
            return affectedServices;
        }

        public List<String> getBreachDetails() {
            return breachDetails;
        }

        @Override
        public String toString() {
            return String.format(
                "Breach[time=%s, pods=%d, namespaces=%d, services=%d, details=%s]",
                timestamp, affectedPods, affectedNamespaces, affectedServices, breachDetails
            );
        }
    }
}

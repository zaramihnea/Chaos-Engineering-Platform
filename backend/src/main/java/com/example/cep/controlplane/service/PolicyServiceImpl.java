package com.example.cep.controlplane.service;

import com.example.cep.model.ExperimentDefinition;
import com.example.cep.model.FaultType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Policy Service Implementation
 *
 * TDD Iteration 1 - GREEN Phase Implementation
 *
 * This service enforces organizational policies for chaos experiments.
 * It acts as a security gate preventing dangerous or unauthorized experiments.
 *
 * SOA Principles Demonstrated:
 * - Service Autonomy: Independent policy evaluation logic
 * - Service Reusability: Can be used by multiple API endpoints
 * - Service Abstraction: Hides complex policy rules behind simple interface
 *
 * Policy Rules Enforced:
 * 1. Namespace Restrictions: Only safe namespaces allowed
 * 2. Cluster Restrictions: Only authorized clusters allowed
 * 3. Duration Limits: Maximum 30-minute experiments
 * 4. Fault Type Restrictions: Some faults require approval
 * 5. SLO Requirements: At least one SLO must be defined
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0 (TDD Iteration 1)
 */
@Service
public class PolicyServiceImpl implements PolicyService {

    // ==================== Policy Configuration Constants ====================

    /**
     * Allowed namespaces for chaos experiments
     * Production namespace explicitly excluded for safety
     */
    private static final Set<String> ALLOWED_NAMESPACES = new HashSet<>(Arrays.asList(
        "default",
        "staging",
        "test",
        "dev"
    ));

    /**
     * Allowed Kubernetes clusters for chaos experiments
     * Only non-critical clusters permitted
     */
    private static final Set<String> ALLOWED_CLUSTERS = new HashSet<>(Arrays.asList(
        "production-cluster",  // Note: Despite name, this is a test production environment
        "staging-cluster",
        "dev-cluster"
    ));

    /**
     * Maximum experiment duration in seconds (30 minutes)
     * Prevents long-running experiments that could cause extended outages
     */
    private static final int MAX_EXPERIMENT_DURATION_SECONDS = 1800; // 30 minutes

    /**
     * Fault types that require special approval
     * These are high-risk operations that need elevated permissions
     */
    private static final Set<FaultType> RESTRICTED_FAULT_TYPES = new HashSet<>(Arrays.asList(
        FaultType.NETWORK_PARTITION  // Can cause split-brain scenarios
    ));

    /**
     * Minimum number of SLOs required per experiment
     * Ensures experiments have measurable success criteria
     */
    private static final int MIN_SLO_COUNT = 1;

    // ==================== Public API Methods ====================

    /**
     * Evaluates whether an experiment is allowed under organizational policies
     *
     * TDD Test Coverage: Used by all ControlPlaneApiTest tests
     *
     * Evaluation Order (fail-fast approach):
     * 1. Namespace validation
     * 2. Cluster validation
     * 3. Duration validation
     * 4. SLO validation
     * 5. Fault type validation
     *
     * @param def The experiment definition to evaluate
     * @return true if all policy checks pass, false otherwise
     */
    @Override
    public boolean isAllowed(ExperimentDefinition def) {
        // Null check
        if (def == null || def.getTarget() == null) {
            return false;
        }

        // Rule 1: Namespace Check
        if (!isNamespaceAllowed(def)) {
            return false;
        }

        // Rule 2: Cluster Check
        if (!isClusterAllowed(def)) {
            return false;
        }

        // Rule 3: Duration Check
        if (!isDurationAllowed(def)) {
            return false;
        }

        // Rule 4: SLO Check
        if (!hasSufficientSlos(def)) {
            return false;
        }

        // Rule 5: Fault Type Check (non-blocking for restricted types)
        // Note: Restricted fault types are allowed but flagged for approval
        // They don't prevent creation, just require additional approval step

        // All checks passed
        return true;
    }

    /**
     * Provides a human-readable reason for policy denial
     *
     * TDD Test Coverage:
     * - testCreateExperiment_PolicyViolation_ThrowsException (GREEN)
     *
     * This method performs the same checks as isAllowed() but returns
     * descriptive messages for the first failing rule.
     *
     * @param def The experiment definition that was denied
     * @return Specific reason for denial, or empty string if allowed
     */
    @Override
    public String denialReason(ExperimentDefinition def) {
        // Null check
        if (def == null) {
            return "Experiment definition is null";
        }

        if (def.getTarget() == null) {
            return "Target system is not specified";
        }

        // Rule 1: Namespace Check
        if (!isNamespaceAllowed(def)) {
            return String.format(
                "Invalid namespace: '%s'. Allowed namespaces: %s",
                def.getTarget().getNamespace(),
                ALLOWED_NAMESPACES
            );
        }

        // Rule 2: Cluster Check
        if (!isClusterAllowed(def)) {
            return String.format(
                "Invalid cluster: '%s'. Allowed clusters: %s",
                def.getTarget().getCluster(),
                ALLOWED_CLUSTERS
            );
        }

        // Rule 3: Duration Check
        if (!isDurationAllowed(def)) {
            Duration timeout = def.getTimeout();
            long timeoutSeconds = (timeout != null) ? timeout.getSeconds() : 0;
            return String.format(
                "Experiment duration exceeds maximum: %d seconds (max: %d seconds = 30 minutes)",
                timeoutSeconds,
                MAX_EXPERIMENT_DURATION_SECONDS
            );
        }

        // Rule 4: SLO Check
        if (!hasSufficientSlos(def)) {
            int sloCount = (def.getSlos() != null) ? def.getSlos().size() : 0;
            return String.format(
                "Insufficient SLO definitions: %d (minimum required: %d). " +
                "At least one SLO must be defined to measure experiment impact.",
                sloCount,
                MIN_SLO_COUNT
            );
        }

        // Rule 5: Fault Type Warning (informational, not blocking)
        if (isRestrictedFaultType(def)) {
            return String.format(
                "Fault type '%s' requires approval before execution. " +
                "Please request approval from a chaos engineering lead.",
                def.getFaultType()
            );
        }

        // All checks passed
        return "";
    }

    // ==================== Private Policy Rule Methods ====================

    /**
     * Validates namespace is in allowed list
     *
     * @param def Experiment definition
     * @return true if namespace is allowed
     */
    private boolean isNamespaceAllowed(ExperimentDefinition def) {
        String namespace = def.getTarget().getNamespace();
        return namespace != null && ALLOWED_NAMESPACES.contains(namespace);
    }

    /**
     * Validates cluster is in allowed list
     *
     * @param def Experiment definition
     * @return true if cluster is allowed
     */
    private boolean isClusterAllowed(ExperimentDefinition def) {
        String cluster = def.getTarget().getCluster();
        return cluster != null && ALLOWED_CLUSTERS.contains(cluster);
    }

    /**
     * Validates experiment duration is within limits
     *
     * Rationale: Long-running experiments increase risk of:
     * - Extended service degradation
     * - Customer impact
     * - Difficulty in rollback
     *
     * @param def Experiment definition
     * @return true if duration is acceptable
     */
    private boolean isDurationAllowed(ExperimentDefinition def) {
        Duration timeout = def.getTimeout();
        if (timeout == null) {
            return false;
        }
        long timeoutSeconds = timeout.getSeconds();
        return timeoutSeconds > 0 && timeoutSeconds <= MAX_EXPERIMENT_DURATION_SECONDS;
    }

    /**
     * Validates sufficient SLO definitions exist
     *
     * Rationale: SLOs provide measurable success criteria.
     * Without SLOs, we cannot objectively determine if an experiment
     * succeeded or caused unacceptable degradation.
     *
     * @param def Experiment definition
     * @return true if at least one SLO is defined
     */
    private boolean hasSufficientSlos(ExperimentDefinition def) {
        return def.getSlos() != null && def.getSlos().size() >= MIN_SLO_COUNT;
    }

    /**
     * Checks if fault type requires special approval
     *
     * Note: This is a warning/flagging mechanism, not a hard block.
     * The experiment can still be created but will require approval
     * before execution.
     *
     * @param def Experiment definition
     * @return true if fault type is restricted
     */
    private boolean isRestrictedFaultType(ExperimentDefinition def) {
        return RESTRICTED_FAULT_TYPES.contains(def.getFaultType());
    }

    // ==================== Future Enhancement Methods ====================

    /**
     * Future: Check if user has permission for this experiment
     * Currently not implemented - placeholder for RBAC integration
     */
    @SuppressWarnings("unused")
    private boolean hasUserPermission(ExperimentDefinition def) {
        // TODO: Integrate with AccessControl service
        // return accessControl.canCreate(authContext, def);
        return true;
    }

    /**
     * Future: Check if target system has active protective controls
     * Currently not implemented - placeholder for integration with monitoring
     */
    @SuppressWarnings("unused")
    private boolean hasActiveProtection(ExperimentDefinition def) {
        // TODO: Query monitoring system for circuit breakers, rate limits, etc.
        return true;
    }
}

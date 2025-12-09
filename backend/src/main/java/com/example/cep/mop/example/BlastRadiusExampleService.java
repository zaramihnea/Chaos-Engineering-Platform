package com.example.cep.mop.example;

import com.example.cep.mop.annotations.MonitorBlastRadius;
import com.example.cep.model.RunPlan;
import org.springframework.stereotype.Service;

/**
 * Example service demonstrating Blast Radius Monitoring usage
 *
 * This service shows how to use the @MonitorBlastRadius annotation
 * to automatically track and limit the impact of chaos experiments.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Service
public class BlastRadiusExampleService {

    /**
     * Example 1: Safe pod kill operation
     *
     * This method demonstrates successful execution where the blast radius
     * stays within defined limits (max 2 pods).
     *
     * @param plan Run plan for the experiment
     * @return Success message
     * @throws InterruptedException if sleep is interrupted
     */
    @MonitorBlastRadius(
        maxAffectedPods = 2,
        maxAffectedNamespaces = 1,
        maxAffectedServices = 1,
        checkIntervalSeconds = 2,
        abortOnBreach = true,
        logAllChecks = true
    )
    public String executeSafePodKill(RunPlan plan) throws InterruptedException {
        System.out.println("🔧 Starting pod kill operation...");
        System.out.println("   Target: " + plan.getDefinition().getId());

        // Simulate fault injection taking some time
        System.out.println("\n   ⚡ Injecting pod-kill fault...");
        Thread.sleep(3000); // 3 seconds

        System.out.println("   ✅ Fault injection completed");
        System.out.println("   📊 Blast radius will be monitored in background\n");

        // Simulate some additional work
        Thread.sleep(2000); // 2 more seconds

        return "Pod kill operation completed successfully";
    }

    /**
     * Example 2: Unsafe operation that breaches blast radius
     *
     * This method demonstrates what happens when a fault spreads beyond
     * intended limits. The monitoring will detect the breach and abort.
     *
     * Note: In the test, we'll simulate a breach by configuring the
     * BlastRadiusService to discover more pods than allowed.
     *
     * @param plan Run plan for the experiment
     * @return Success message (will not be reached if breach occurs)
     * @throws InterruptedException if sleep is interrupted
     */
    @MonitorBlastRadius(
        maxAffectedPods = 1,
        maxAffectedNamespaces = 1,
        maxAffectedServices = 1,
        checkIntervalSeconds = 2,
        abortOnBreach = true,
        autoRollback = true,
        logAllChecks = true
    )
    public String executeUnsafePodKill(RunPlan plan) throws InterruptedException {
        System.out.println("🔧 Starting pod kill operation (UNSAFE - will breach)...");
        System.out.println("   Target: " + plan.getDefinition().getId());

        // Simulate fault injection
        System.out.println("\n   ⚡ Injecting pod-kill fault...");
        Thread.sleep(3000); // 3 seconds

        System.out.println("   💥 Fault spreading beyond intended target!");
        System.out.println("   📊 Monitoring will detect breach...\n");

        // Give time for monitoring to detect the breach
        Thread.sleep(3000); // 3 more seconds

        // This line should NOT be reached if monitoring works correctly
        return "This should not be returned - operation should be aborted!";
    }

    /**
     * Example 3: Multi-service experiment with larger blast radius
     *
     * This demonstrates monitoring a more complex experiment that
     * affects multiple services but still has safety limits.
     *
     * @param plan Run plan for the experiment
     * @return Success message
     * @throws InterruptedException if sleep is interrupted
     */
    @MonitorBlastRadius(
        maxAffectedPods = 5,
        maxAffectedNamespaces = 2,
        maxAffectedServices = 3,
        checkIntervalSeconds = 3,
        abortOnBreach = true,
        logAllChecks = false  // Only log breaches
    )
    public String executeMultiServiceExperiment(RunPlan plan) throws InterruptedException {
        System.out.println("🔧 Starting multi-service chaos experiment...");
        System.out.println("   Target: " + plan.getDefinition().getId());

        // Simulate complex fault injection
        System.out.println("\n   ⚡ Phase 1: Network latency injection...");
        Thread.sleep(2000);

        System.out.println("   ⚡ Phase 2: CPU throttling...");
        Thread.sleep(2000);

        System.out.println("   ⚡ Phase 3: Memory pressure...");
        Thread.sleep(2000);

        System.out.println("   ✅ Multi-service experiment completed\n");

        return "Multi-service experiment completed successfully";
    }
}

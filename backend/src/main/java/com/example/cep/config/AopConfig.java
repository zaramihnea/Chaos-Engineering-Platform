package com.example.cep.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class to enable Aspect-Oriented Programming (AOP) in Spring Boot
 *
 * This configuration activates Spring's AOP support, allowing @Aspect annotated
 * classes to function as aspects that intercept method calls and apply cross-cutting
 * concerns like SLO validation and monitoring.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * SPRING AOP CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * @EnableAspectJAutoProxy:
 * - Enables Spring's support for handling components marked with @Aspect
 * - Creates proxy objects around beans that have methods matching pointcut expressions
 * - Supports both JDK dynamic proxies and CGLIB proxies
 *
 * proxyTargetClass = true:
 * - Forces CGLIB proxies instead of JDK dynamic proxies
 * - CGLIB creates subclass-based proxies (works with classes, not just interfaces)
 * - Required for proxying methods in classes without interfaces
 * - Recommended for Spring Boot applications
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * PROXY MECHANISMS EXPLAINED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * JDK Dynamic Proxies (proxyTargetClass = false):
 * - Only works with interfaces
 * - Creates proxy that implements the same interface
 * - Faster proxy creation, but limited to interface methods
 *
 * CGLIB Proxies (proxyTargetClass = true):
 * - Works with concrete classes
 * - Creates proxy as a subclass of the target class
 * - Slightly slower proxy creation, but more flexible
 * - Can intercept both interface and class methods
 *
 * Example with our platform:
 * ┌────────────────────────────┐
 * │ OrchestratorServiceImpl    │  ← Original class
 * └────────────────────────────┘
 *              ↓
 * ┌────────────────────────────┐
 * │ OrchestratorServiceImpl$$  │  ← CGLIB proxy (subclass)
 * │ EnhancerBySpringCGLIB      │
 * └────────────────────────────┘
 *              ↓
 *      Intercepts calls to
 *      @ValidateSlo methods
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * HOW ASPECTS ARE APPLIED
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. Spring scans for @Aspect annotated classes (e.g., SloValidationAspect)
 * 2. Spring scans for beans with methods matching aspect pointcuts
 * 3. For each matching bean, Spring creates a proxy
 * 4. When a method is called, the call goes through the proxy first
 * 5. Proxy executes aspect advice (before/after/around)
 * 6. Proxy then delegates to the actual target method
 *
 * Call Flow Example:
 * Client → Proxy → Aspect (before) → Target Method → Aspect (after) → Return
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * INTEGRATION WITH CHAOS ENGINEERING PLATFORM
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * This configuration enables:
 * - SloValidationAspect to intercept experiment dispatch calls
 * - SloMonitoringAspect to wrap long-running experiment executions
 * - Automatic SLO validation without modifying business logic
 * - Centralized cross-cutting concern management
 *
 * Without this configuration, @Aspect classes would be ignored and SLO validation
 * would need to be manually added to every service method.
 *
 * @author Zară Mihnea-Tudor
 * @version 1.0
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {

    /**
     * Bean definition marker
     *
     * This class doesn't need any bean definitions because @EnableAspectJAutoProxy
     * automatically registers all necessary infrastructure beans.
     *
     * Spring automatically:
     * - Creates AnnotationAwareAspectJAutoProxyCreator
     * - Scans for @Aspect annotated classes
     * - Creates proxy beans for classes with matching methods
     */

    // Additional AOP-related beans can be defined here if needed
    // For example: custom AspectJ expression parsers, advice listeners, etc.
}

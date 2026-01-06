package com.example.cep.mop.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Blast Radius State Tests")
class BlastRadiusStateTest {

    private BlastRadiusState state;
    private static final String TEST_EXPERIMENT_ID = "exp-001";

    @BeforeEach
    void setUp() {
        state = new BlastRadiusState(TEST_EXPERIMENT_ID);
    }

    @Test
    @DisplayName("constructor initializes with experiment ID")
    void testConstructor_InitializesExperimentId() {
        assertEquals(TEST_EXPERIMENT_ID, state.getExperimentId());
    }

    @Test
    @DisplayName("constructor initializes empty collections")
    void testConstructor_InitializesEmptyCollections() {
        assertEquals(0, state.getAffectedPodCount());
        assertEquals(0, state.getAffectedNamespaceCount());
        assertEquals(0, state.getAffectedServiceCount());
        assertTrue(state.getAffectedPods().isEmpty());
        assertTrue(state.getAffectedNamespaces().isEmpty());
        assertTrue(state.getAffectedServices().isEmpty());
        assertTrue(state.getMetadata().isEmpty());
    }

    @Test
    @DisplayName("constructor sets capture time")
    void testConstructor_SetsCaptureTime() {
        assertNotNull(state.getCaptureTime());
    }

    @Test
    @DisplayName("addAffectedPod adds pod to collection")
    void testAddAffectedPod() {
        state.addAffectedPod("pod-1");

        assertEquals(1, state.getAffectedPodCount());
        assertTrue(state.getAffectedPods().contains("pod-1"));
    }

    @Test
    @DisplayName("addAffectedPod handles multiple pods")
    void testAddAffectedPod_MultiplePods() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedPod("pod-3");

        assertEquals(3, state.getAffectedPodCount());
        Set<String> pods = state.getAffectedPods();
        assertTrue(pods.contains("pod-1"));
        assertTrue(pods.contains("pod-2"));
        assertTrue(pods.contains("pod-3"));
    }

    @Test
    @DisplayName("addAffectedPod handles duplicate pods")
    void testAddAffectedPod_Duplicates() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-1");

        assertEquals(1, state.getAffectedPodCount());
    }

    @Test
    @DisplayName("addAffectedNamespace adds namespace to collection")
    void testAddAffectedNamespace() {
        state.addAffectedNamespace("default");

        assertEquals(1, state.getAffectedNamespaceCount());
        assertTrue(state.getAffectedNamespaces().contains("default"));
    }

    @Test
    @DisplayName("addAffectedNamespace handles multiple namespaces")
    void testAddAffectedNamespace_MultipleNamespaces() {
        state.addAffectedNamespace("default");
        state.addAffectedNamespace("staging");
        state.addAffectedNamespace("production");

        assertEquals(3, state.getAffectedNamespaceCount());
        Set<String> namespaces = state.getAffectedNamespaces();
        assertTrue(namespaces.contains("default"));
        assertTrue(namespaces.contains("staging"));
        assertTrue(namespaces.contains("production"));
    }

    @Test
    @DisplayName("addAffectedService adds service to collection")
    void testAddAffectedService() {
        state.addAffectedService("service-1");

        assertEquals(1, state.getAffectedServiceCount());
        assertTrue(state.getAffectedServices().contains("service-1"));
    }

    @Test
    @DisplayName("addAffectedService handles multiple services")
    void testAddAffectedService_MultipleServices() {
        state.addAffectedService("service-1");
        state.addAffectedService("service-2");

        assertEquals(2, state.getAffectedServiceCount());
        Set<String> services = state.getAffectedServices();
        assertTrue(services.contains("service-1"));
        assertTrue(services.contains("service-2"));
    }

    @Test
    @DisplayName("addMetadata adds metadata entry")
    void testAddMetadata() {
        state.addMetadata("key1", "value1");

        Map<String, Object> metadata = state.getMetadata();
        assertEquals("value1", metadata.get("key1"));
    }

    @Test
    @DisplayName("addMetadata handles multiple entries")
    void testAddMetadata_MultipleEntries() {
        state.addMetadata("key1", "value1");
        state.addMetadata("key2", 123);
        state.addMetadata("key3", true);

        Map<String, Object> metadata = state.getMetadata();
        assertEquals("value1", metadata.get("key1"));
        assertEquals(123, metadata.get("key2"));
        assertEquals(true, metadata.get("key3"));
    }

    @Test
    @DisplayName("exceedsThresholds returns false when within limits")
    void testExceedsThresholds_WithinLimits() {
        state.addAffectedPod("pod-1");
        state.addAffectedNamespace("default");
        state.addAffectedService("service-1");

        assertFalse(state.exceedsThresholds(5, 5, 5));
    }

    @Test
    @DisplayName("exceedsThresholds returns true when pods exceed limit")
    void testExceedsThresholds_PodsExceedLimit() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedPod("pod-3");

        assertTrue(state.exceedsThresholds(2, 5, 5));
    }

    @Test
    @DisplayName("exceedsThresholds returns true when namespaces exceed limit")
    void testExceedsThresholds_NamespacesExceedLimit() {
        state.addAffectedNamespace("ns-1");
        state.addAffectedNamespace("ns-2");
        state.addAffectedNamespace("ns-3");

        assertTrue(state.exceedsThresholds(5, 2, 5));
    }

    @Test
    @DisplayName("exceedsThresholds returns true when services exceed limit")
    void testExceedsThresholds_ServicesExceedLimit() {
        state.addAffectedService("svc-1");
        state.addAffectedService("svc-2");
        state.addAffectedService("svc-3");

        assertTrue(state.exceedsThresholds(5, 5, 2));
    }

    @Test
    @DisplayName("exceedsThresholds returns true when multiple exceed")
    void testExceedsThresholds_MultipleExceed() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedNamespace("ns-1");
        state.addAffectedNamespace("ns-2");

        assertTrue(state.exceedsThresholds(1, 1, 5));
    }

    @Test
    @DisplayName("getBreaches returns empty list when within limits")
    void testGetBreaches_WithinLimits() {
        state.addAffectedPod("pod-1");

        List<String> breaches = state.getBreaches(5, 5, 5);
        assertTrue(breaches.isEmpty());
    }

    @Test
    @DisplayName("getBreaches returns pod breach")
    void testGetBreaches_PodBreach() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedPod("pod-3");

        List<String> breaches = state.getBreaches(2, 5, 5);
        assertEquals(1, breaches.size());
        assertTrue(breaches.get(0).contains("Pods"));
        assertTrue(breaches.get(0).contains("3"));
        assertTrue(breaches.get(0).contains("2"));
    }

    @Test
    @DisplayName("getBreaches returns namespace breach")
    void testGetBreaches_NamespaceBreach() {
        state.addAffectedNamespace("ns-1");
        state.addAffectedNamespace("ns-2");

        List<String> breaches = state.getBreaches(5, 1, 5);
        assertEquals(1, breaches.size());
        assertTrue(breaches.get(0).contains("Namespaces"));
    }

    @Test
    @DisplayName("getBreaches returns service breach")
    void testGetBreaches_ServiceBreach() {
        state.addAffectedService("svc-1");
        state.addAffectedService("svc-2");
        state.addAffectedService("svc-3");

        List<String> breaches = state.getBreaches(5, 5, 2);
        assertEquals(1, breaches.size());
        assertTrue(breaches.get(0).contains("Services"));
    }

    @Test
    @DisplayName("getBreaches returns multiple breaches")
    void testGetBreaches_MultipleBreaches() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedNamespace("ns-1");
        state.addAffectedNamespace("ns-2");
        state.addAffectedService("svc-1");
        state.addAffectedService("svc-2");

        List<String> breaches = state.getBreaches(1, 1, 1);
        assertEquals(3, breaches.size());
    }

    @Test
    @DisplayName("getAffectedPods returns unmodifiable set")
    void testGetAffectedPods_Unmodifiable() {
        state.addAffectedPod("pod-1");
        Set<String> pods = state.getAffectedPods();

        assertThrows(UnsupportedOperationException.class, () -> {
            pods.add("pod-2");
        });
    }

    @Test
    @DisplayName("getAffectedNamespaces returns unmodifiable set")
    void testGetAffectedNamespaces_Unmodifiable() {
        state.addAffectedNamespace("ns-1");
        Set<String> namespaces = state.getAffectedNamespaces();

        assertThrows(UnsupportedOperationException.class, () -> {
            namespaces.add("ns-2");
        });
    }

    @Test
    @DisplayName("getAffectedServices returns unmodifiable set")
    void testGetAffectedServices_Unmodifiable() {
        state.addAffectedService("svc-1");
        Set<String> services = state.getAffectedServices();

        assertThrows(UnsupportedOperationException.class, () -> {
            services.add("svc-2");
        });
    }

    @Test
    @DisplayName("getMetadata returns unmodifiable map")
    void testGetMetadata_Unmodifiable() {
        state.addMetadata("key1", "value1");
        Map<String, Object> metadata = state.getMetadata();

        assertThrows(UnsupportedOperationException.class, () -> {
            metadata.put("key2", "value2");
        });
    }

    @Test
    @DisplayName("toString includes experiment ID and counts")
    void testToString() {
        state.addAffectedPod("pod-1");
        state.addAffectedNamespace("ns-1");
        state.addAffectedService("svc-1");

        String str = state.toString();
        assertTrue(str.contains(TEST_EXPERIMENT_ID));
        assertTrue(str.contains("pods=1"));
        assertTrue(str.contains("namespaces=1"));
        assertTrue(str.contains("services=1"));
    }

    @Test
    @DisplayName("getDetailedSummary includes all information")
    void testGetDetailedSummary() {
        state.addAffectedPod("pod-1");
        state.addAffectedPod("pod-2");
        state.addAffectedNamespace("default");
        state.addAffectedService("service-1");

        String summary = state.getDetailedSummary();
        assertTrue(summary.contains(TEST_EXPERIMENT_ID));
        assertTrue(summary.contains("pod-1"));
        assertTrue(summary.contains("pod-2"));
        assertTrue(summary.contains("default"));
        assertTrue(summary.contains("service-1"));
    }

    @Test
    @DisplayName("getDetailedSummary handles empty collections")
    void testGetDetailedSummary_EmptyCollections() {
        String summary = state.getDetailedSummary();
        assertTrue(summary.contains(TEST_EXPERIMENT_ID));
        // The summary always shows the labels with count 0 when empty
        assertTrue(summary.contains("Affected Pods:       0"));
        assertTrue(summary.contains("Affected Namespaces: 0"));
        assertTrue(summary.contains("Affected Services:   0"));
    }

    @Test
    @DisplayName("copy constructor creates independent copy")
    void testCopyConstructor() {
        state.addAffectedPod("pod-1");
        state.addAffectedNamespace("ns-1");
        state.addAffectedService("svc-1");
        state.addMetadata("key1", "value1");

        BlastRadiusState copy = new BlastRadiusState(state);

        assertEquals(state.getExperimentId(), copy.getExperimentId());
        assertEquals(state.getAffectedPodCount(), copy.getAffectedPodCount());
        assertEquals(state.getAffectedNamespaceCount(), copy.getAffectedNamespaceCount());
        assertEquals(state.getAffectedServiceCount(), copy.getAffectedServiceCount());
        assertTrue(copy.getAffectedPods().contains("pod-1"));
        assertTrue(copy.getAffectedNamespaces().contains("ns-1"));
        assertTrue(copy.getAffectedServices().contains("svc-1"));
        assertEquals("value1", copy.getMetadata().get("key1"));
    }

    @Test
    @DisplayName("copy constructor creates independent collections")
    void testCopyConstructor_Independence() {
        state.addAffectedPod("pod-1");

        BlastRadiusState copy = new BlastRadiusState(state);

        copy.addAffectedPod("pod-2");

        assertEquals(1, state.getAffectedPodCount());
        assertEquals(2, copy.getAffectedPodCount());
    }

    @Test
    @DisplayName("copy constructor creates new capture time")
    void testCopyConstructor_NewCaptureTime() throws InterruptedException {
        BlastRadiusState copy = new BlastRadiusState(state);

        assertNotEquals(state.getCaptureTime(), copy.getCaptureTime());
    }
}

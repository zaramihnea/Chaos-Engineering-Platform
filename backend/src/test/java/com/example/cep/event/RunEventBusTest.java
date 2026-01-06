package com.example.cep.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RunEventBus
 */
@DisplayName("RunEventBus Tests")
class RunEventBusTest {

    private RunEventBus eventBus;
    private TestObserver observer;

    @BeforeEach
    void setUp() {
        eventBus = new RunEventBus();
        observer = new TestObserver();
    }

    @Test
    @DisplayName("attach observer to event bus")
    void testAttach() {
        assertDoesNotThrow(() -> eventBus.attach(observer));
    }

    @Test
    @DisplayName("detach observer from event bus")
    void testDetach() {
        eventBus.attach(observer);
        assertDoesNotThrow(() -> eventBus.detach(observer));
    }

    @Test
    @DisplayName("notifyObservers with event and data")
    void testNotifyObservers() {
        eventBus.attach(observer);
        Map<String, Object> data = new HashMap<>();
        data.put("runId", "run-123");
        data.put("status", "RUNNING");

        assertDoesNotThrow(() -> eventBus.notifyObservers("RUN_STARTED", data));
    }

    @Test
    @DisplayName("attach null observer")
    void testAttachNullObserver() {
        assertDoesNotThrow(() -> eventBus.attach(null));
    }

    @Test
    @DisplayName("detach null observer")
    void testDetachNullObserver() {
        assertDoesNotThrow(() -> eventBus.detach(null));
    }

    @Test
    @DisplayName("notifyObservers with null event")
    void testNotifyObserversNullEvent() {
        eventBus.attach(observer);
        Map<String, Object> data = Map.of("key", "value");

        assertDoesNotThrow(() -> eventBus.notifyObservers(null, data));
    }

    @Test
    @DisplayName("notifyObservers with null data")
    void testNotifyObserversNullData() {
        eventBus.attach(observer);

        assertDoesNotThrow(() -> eventBus.notifyObservers("TEST_EVENT", null));
    }

    @Test
    @DisplayName("notifyObservers with empty data")
    void testNotifyObserversEmptyData() {
        eventBus.attach(observer);
        Map<String, Object> data = new HashMap<>();

        assertDoesNotThrow(() -> eventBus.notifyObservers("EMPTY_DATA_EVENT", data));
    }

    @Test
    @DisplayName("attach multiple observers")
    void testAttachMultipleObservers() {
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();
        TestObserver observer3 = new TestObserver();

        assertDoesNotThrow(() -> {
            eventBus.attach(observer1);
            eventBus.attach(observer2);
            eventBus.attach(observer3);
        });
    }

    @Test
    @DisplayName("attach same observer multiple times")
    void testAttachSameObserverMultipleTimes() {
        assertDoesNotThrow(() -> {
            eventBus.attach(observer);
            eventBus.attach(observer);
            eventBus.attach(observer);
        });
    }

    @Test
    @DisplayName("detach observer that was never attached")
    void testDetachNonAttachedObserver() {
        TestObserver neverAttached = new TestObserver();
        assertDoesNotThrow(() -> eventBus.detach(neverAttached));
    }

    @Test
    @DisplayName("notifyObservers without any attached observers")
    void testNotifyWithoutObservers() {
        Map<String, Object> data = Map.of("test", "data");
        assertDoesNotThrow(() -> eventBus.notifyObservers("NO_OBSERVERS", data));
    }

    @Test
    @DisplayName("notifyObservers with complex data")
    void testNotifyObserversComplexData() {
        eventBus.attach(observer);
        Map<String, Object> data = new HashMap<>();
        data.put("runId", "run-456");
        data.put("status", "COMPLETED");
        data.put("duration", 1500L);
        data.put("sloViolations", 2);
        data.put("metadata", Map.of("user", "alice", "env", "prod"));

        assertDoesNotThrow(() -> eventBus.notifyObservers("RUN_COMPLETED", data));
    }

    @Test
    @DisplayName("RunEventBus implements Subject interface")
    void testImplementsSubject() {
        assertTrue(eventBus instanceof Subject);
    }

    /**
     * Test implementation of Observer for testing purposes
     */
    private static class TestObserver implements Observer {
        @Override
        public void update(String event, Map<String, Object> data) {
            // Test observer implementation
        }
    }
}

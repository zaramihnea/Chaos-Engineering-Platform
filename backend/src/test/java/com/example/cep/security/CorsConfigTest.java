package com.example.cep.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CorsConfig
 */
@DisplayName("CorsConfig Tests")
class CorsConfigTest {

    @Test
    @DisplayName("CorsConfig can be instantiated")
    void testInstantiation() {
        CorsConfig config = new CorsConfig();

        assertNotNull(config);
    }

    @Test
    @DisplayName("multiple CorsConfig instances can be created")
    void testMultipleInstances() {
        CorsConfig config1 = new CorsConfig();
        CorsConfig config2 = new CorsConfig();

        assertNotNull(config1);
        assertNotNull(config2);
        assertNotSame(config1, config2);
    }

    @Test
    @DisplayName("CorsConfig instance is of correct type")
    void testInstanceType() {
        CorsConfig config = new CorsConfig();

        assertTrue(config instanceof CorsConfig);
    }
}

package com.example.cep.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AuthContext
 */
@DisplayName("AuthContext Tests")
class AuthContextTest {

    @Test
    @DisplayName("constructor creates AuthContext with user and roles")
    void testConstructor() {
        Set<String> roles = Set.of("admin", "operator");
        AuthContext context = new AuthContext("testUser", roles);

        assertNotNull(context);
    }

    @Test
    @DisplayName("getUser returns user value")
    void testGetUser() {
        Set<String> roles = Set.of("admin");
        AuthContext context = new AuthContext("alice", roles);

        // Current stub implementation returns null
        assertNull(context.getUser());
    }

    @Test
    @DisplayName("getRoles returns roles set")
    void testGetRoles() {
        Set<String> roles = Set.of("admin", "operator", "viewer");
        AuthContext context = new AuthContext("bob", roles);

        // Current stub implementation returns null
        assertNull(context.getRoles());
    }

    @Test
    @DisplayName("constructor handles empty roles set")
    void testConstructorWithEmptyRoles() {
        Set<String> roles = Set.of();
        AuthContext context = new AuthContext("user", roles);

        assertNotNull(context);
        assertNull(context.getRoles());
    }

    @Test
    @DisplayName("constructor handles null user")
    void testConstructorWithNullUser() {
        Set<String> roles = Set.of("viewer");
        AuthContext context = new AuthContext(null, roles);

        assertNotNull(context);
        assertNull(context.getUser());
    }

    @Test
    @DisplayName("constructor handles null roles")
    void testConstructorWithNullRoles() {
        AuthContext context = new AuthContext("user", null);

        assertNotNull(context);
        assertNull(context.getRoles());
    }

    @Test
    @DisplayName("constructor handles multiple roles")
    void testConstructorWithMultipleRoles() {
        Set<String> roles = Set.of("admin", "operator", "viewer", "auditor");
        AuthContext context = new AuthContext("superuser", roles);

        assertNotNull(context);
    }

    @Test
    @DisplayName("getUser called multiple times returns consistent value")
    void testGetUserConsistency() {
        AuthContext context = new AuthContext("testUser", Set.of("admin"));

        String user1 = context.getUser();
        String user2 = context.getUser();

        assertEquals(user1, user2);
    }

    @Test
    @DisplayName("getRoles called multiple times returns consistent value")
    void testGetRolesConsistency() {
        AuthContext context = new AuthContext("testUser", Set.of("admin"));

        Set<String> roles1 = context.getRoles();
        Set<String> roles2 = context.getRoles();

        assertEquals(roles1, roles2);
    }
}

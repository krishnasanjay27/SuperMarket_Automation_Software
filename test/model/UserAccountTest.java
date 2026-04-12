package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link UserAccount} model class.
 */
@DisplayName("UserAccount Model Tests")
class UserAccountTest {

    @Test
    @DisplayName("No-arg constructor creates UserAccount with null fields")
    void noArgConstructor_fieldsAreDefaults() {
        UserAccount u = new UserAccount();

        assertNull(u.getUserId());
        assertNull(u.getPassword());
        assertNull(u.getRole());
        assertNull(u.getCreatedAt());
        assertNull(u.getStatus());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        UserAccount u = new UserAccount("MGR001", "hashedPwd", "Manager", now, "ACTIVE");

        assertEquals("MGR001", u.getUserId());
        assertEquals("hashedPwd", u.getPassword());
        assertEquals("Manager", u.getRole());
        assertEquals(now, u.getCreatedAt());
        assertEquals("ACTIVE", u.getStatus());
    }

    @Test
    @DisplayName("Setters update each field")
    void setters_updateFieldsCorrectly() {
        UserAccount u = new UserAccount();
        u.setUserId("SALES001");
        u.setPassword("pwd123");
        u.setRole("SalesStaff");
        u.setStatus("INACTIVE");

        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 0, 0);
        u.setCreatedAt(dt);

        assertEquals("SALES001", u.getUserId());
        assertEquals("pwd123", u.getPassword());
        assertEquals("SalesStaff", u.getRole());
        assertEquals("INACTIVE", u.getStatus());
        assertEquals(dt, u.getCreatedAt());
    }

    @Test
    @DisplayName("toString() excludes password for a degree of safety")
    void toString_containsFields() {
        UserAccount u = new UserAccount("INV001", "secret", "InventoryStaff",
                                        LocalDateTime.now(), "ACTIVE");
        String str = u.toString();

        assertTrue(str.contains("INV001"));
        assertTrue(str.contains("InventoryStaff"));
        assertTrue(str.contains("ACTIVE"));
        // Password is NOT in toString
        assertFalse(str.contains("secret"));
    }
}

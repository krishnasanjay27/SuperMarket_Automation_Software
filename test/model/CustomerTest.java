package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Customer} model class.
 */
@DisplayName("Customer Model Tests")
class CustomerTest {

    @Test
    @DisplayName("No-arg constructor creates Customer with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        Customer c = new Customer();

        assertEquals(0, c.getCustomerId());
        assertNull(c.getName());
        assertNull(c.getPhone());
        assertNull(c.getEmail());
        assertNull(c.getAddress());
        assertEquals(0, c.getLoyaltyPoints());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        Customer c = new Customer(1, "John Doe", "9876543210",
                                  "john@example.com", "123 Main St", 500);

        assertEquals(1, c.getCustomerId());
        assertEquals("John Doe", c.getName());
        assertEquals("9876543210", c.getPhone());
        assertEquals("john@example.com", c.getEmail());
        assertEquals("123 Main St", c.getAddress());
        assertEquals(500, c.getLoyaltyPoints());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void setters_updateFieldsCorrectly() {
        Customer c = new Customer();
        c.setCustomerId(10);
        c.setName("Jane");
        c.setPhone("1234567890");
        c.setEmail("jane@test.com");
        c.setAddress("456 Elm St");
        c.setLoyaltyPoints(100);

        assertEquals(10, c.getCustomerId());
        assertEquals("Jane", c.getName());
        assertEquals("1234567890", c.getPhone());
        assertEquals("jane@test.com", c.getEmail());
        assertEquals("456 Elm St", c.getAddress());
        assertEquals(100, c.getLoyaltyPoints());
    }

    @Test
    @DisplayName("Loyalty points can be zero")
    void loyaltyPoints_canBeZero() {
        Customer c = new Customer(1, "Test", "0000000000", null, null, 0);
        assertEquals(0, c.getLoyaltyPoints());
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        Customer c = new Customer(5, "Alice", "1111111111", "a@b.com", "Addr", 200);
        String str = c.toString();

        assertTrue(str.contains("Alice"));
        assertTrue(str.contains("1111111111"));
        assertTrue(str.contains("200"));
    }
}

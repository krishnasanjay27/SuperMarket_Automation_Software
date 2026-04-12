package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Item} model class.
 * Validates constructors, getters, setters, default values, and toString().
 */
@DisplayName("Item Model Tests")
class ItemTest {

    @Test
    @DisplayName("No-arg constructor creates an Item with null/default fields")
    void noArgConstructor_fieldsAreDefaults() {
        Item item = new Item();

        assertNull(item.getItemCode());
        assertNull(item.getItemName());
        assertEquals(0.0, item.getPrice());
        assertEquals(0.0, item.getCostPrice());
        assertEquals(0, item.getReorderLevel());
        assertNull(item.getCategory());
        assertNull(item.getCreatedAt());
        assertNull(item.getVendorId());
        assertEquals(0, item.getReturnDurationDays());
    }

    @Test
    @DisplayName("Parameterised constructor sets all core fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 1, 10, 30);
        Item item = new Item("ITM001", "Basmati Rice 5kg", 299.99, 220.00,
                             20, "Grains & Rice", now);

        assertEquals("ITM001", item.getItemCode());
        assertEquals("Basmati Rice 5kg", item.getItemName());
        assertEquals(299.99, item.getPrice(), 0.001);
        assertEquals(220.00, item.getCostPrice(), 0.001);
        assertEquals(20, item.getReorderLevel());
        assertEquals("Grains & Rice", item.getCategory());
        assertEquals(now, item.getCreatedAt());
    }

    @Test
    @DisplayName("Setters update each field correctly")
    void setters_updateFieldsCorrectly() {
        Item item = new Item();

        item.setItemCode("ITM099");
        item.setItemName("Test Item");
        item.setPrice(50.0);
        item.setCostPrice(30.0);
        item.setReorderLevel(5);
        item.setCategory("Test");
        item.setVendorId(42);
        item.setReturnDurationDays(7);

        LocalDateTime dt = LocalDateTime.now();
        item.setCreatedAt(dt);

        assertEquals("ITM099", item.getItemCode());
        assertEquals("Test Item", item.getItemName());
        assertEquals(50.0, item.getPrice());
        assertEquals(30.0, item.getCostPrice());
        assertEquals(5, item.getReorderLevel());
        assertEquals("Test", item.getCategory());
        assertEquals(42, item.getVendorId());
        assertEquals(7, item.getReturnDurationDays());
        assertEquals(dt, item.getCreatedAt());
    }

    @Test
    @DisplayName("VendorId can be set to null")
    void vendorId_canBeNull() {
        Item item = new Item();
        item.setVendorId(10);
        assertEquals(10, item.getVendorId());
        item.setVendorId(null);
        assertNull(item.getVendorId());
    }

    @Test
    @DisplayName("toString() contains key field values")
    void toString_containsKeyFields() {
        Item item = new Item("ITM001", "Rice", 100.0, 80.0,
                             10, "Grains", LocalDateTime.now());
        String str = item.toString();

        assertTrue(str.contains("ITM001"));
        assertTrue(str.contains("Rice"));
        assertTrue(str.contains("100.0"));
        assertTrue(str.contains("80.0"));
        assertTrue(str.contains("Grains"));
    }

    @Test
    @DisplayName("Price and cost price accept zero")
    void priceAndCostPrice_acceptZero() {
        Item item = new Item();
        item.setPrice(0.0);
        item.setCostPrice(0.0);
        assertEquals(0.0, item.getPrice());
        assertEquals(0.0, item.getCostPrice());
    }
}

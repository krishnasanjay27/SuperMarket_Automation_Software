package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link InventoryRecord} model class.
 */
@DisplayName("InventoryRecord Model Tests")
class InventoryRecordTest {

    @Test
    @DisplayName("No-arg constructor creates InventoryRecord with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        InventoryRecord ir = new InventoryRecord();

        assertEquals(0, ir.getInventoryId());
        assertNull(ir.getItemCode());
        assertEquals(0, ir.getStockLevel());
        assertNull(ir.getLastUpdated());
        assertNull(ir.getUpdatedBy());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        InventoryRecord ir = new InventoryRecord(1, "ITM001", 150, now, "INV001");

        assertEquals(1, ir.getInventoryId());
        assertEquals("ITM001", ir.getItemCode());
        assertEquals(150, ir.getStockLevel());
        assertEquals(now, ir.getLastUpdated());
        assertEquals("INV001", ir.getUpdatedBy());
    }

    @Test
    @DisplayName("Stock level can be zero")
    void stockLevel_canBeZero() {
        InventoryRecord ir = new InventoryRecord();
        ir.setStockLevel(0);
        assertEquals(0, ir.getStockLevel());
    }

    @Test
    @DisplayName("Setters update each field")
    void setters_updateFields() {
        InventoryRecord ir = new InventoryRecord();
        LocalDateTime dt = LocalDateTime.of(2026, 6, 15, 12, 0);

        ir.setInventoryId(42);
        ir.setItemCode("ITM005");
        ir.setStockLevel(75);
        ir.setLastUpdated(dt);
        ir.setUpdatedBy("SALES001");

        assertEquals(42, ir.getInventoryId());
        assertEquals("ITM005", ir.getItemCode());
        assertEquals(75, ir.getStockLevel());
        assertEquals(dt, ir.getLastUpdated());
        assertEquals("SALES001", ir.getUpdatedBy());
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        InventoryRecord ir = new InventoryRecord(1, "ITM002", 200,
                                                  LocalDateTime.now(), "INV001");
        String str = ir.toString();

        assertTrue(str.contains("ITM002"));
        assertTrue(str.contains("200"));
        assertTrue(str.contains("INV001"));
    }
}

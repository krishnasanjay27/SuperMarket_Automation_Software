package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PriceHistory} model class.
 */
@DisplayName("PriceHistory Model Tests")
class PriceHistoryTest {

    @Test
    @DisplayName("No-arg constructor creates PriceHistory with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        PriceHistory ph = new PriceHistory();

        assertEquals(0, ph.getPriceHistoryId());
        assertNull(ph.getItemCode());
        assertEquals(0.0, ph.getOldPrice());
        assertEquals(0.0, ph.getNewPrice());
        assertNull(ph.getChangedAt());
        assertNull(ph.getChangedBy());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        PriceHistory ph = new PriceHistory(1, "ITM002", 139.50, 149.50, now, "MGR001");

        assertEquals(1, ph.getPriceHistoryId());
        assertEquals("ITM002", ph.getItemCode());
        assertEquals(139.50, ph.getOldPrice(), 0.001);
        assertEquals(149.50, ph.getNewPrice(), 0.001);
        assertEquals(now, ph.getChangedAt());
        assertEquals("MGR001", ph.getChangedBy());
    }

    @Test
    @DisplayName("Price change difference is calculable")
    void priceChange_differenceIsCalculable() {
        PriceHistory ph = new PriceHistory();
        ph.setOldPrice(100.0);
        ph.setNewPrice(120.0);

        double diff = ph.getNewPrice() - ph.getOldPrice();
        assertEquals(20.0, diff, 0.001);
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        PriceHistory ph = new PriceHistory(5, "ITM003", 40.0, 45.0,
                                           LocalDateTime.now(), "MGR001");
        String str = ph.toString();

        assertTrue(str.contains("ITM003"));
        assertTrue(str.contains("40.0"));
        assertTrue(str.contains("45.0"));
        assertTrue(str.contains("MGR001"));
    }
}

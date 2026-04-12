package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link TransactionItem} model class.
 */
@DisplayName("TransactionItem Model Tests")
class TransactionItemTest {

    @Test
    @DisplayName("No-arg constructor creates TransactionItem with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        TransactionItem ti = new TransactionItem();

        assertEquals(0, ti.getTransactionItemId());
        assertNull(ti.getTransactionId());
        assertNull(ti.getItemCode());
        assertEquals(0, ti.getQuantity());
        assertEquals(0.0, ti.getUnitPrice());
        assertEquals(0.0, ti.getLineTotal());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001",
                                                 2, 299.99, 599.98);

        assertEquals(1, ti.getTransactionItemId());
        assertEquals("TXN001", ti.getTransactionId());
        assertEquals("ITM001", ti.getItemCode());
        assertEquals(2, ti.getQuantity());
        assertEquals(299.99, ti.getUnitPrice(), 0.001);
        assertEquals(599.98, ti.getLineTotal(), 0.001);
    }

    @Test
    @DisplayName("Line total equals quantity * unitPrice for typical usage")
    void lineTotal_matchesQuantityTimesUnitPrice() {
        TransactionItem ti = new TransactionItem();
        ti.setQuantity(3);
        ti.setUnitPrice(45.00);
        ti.setLineTotal(3 * 45.00);

        assertEquals(135.0, ti.getLineTotal(), 0.001);
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        TransactionItem ti = new TransactionItem(5, "TXN999", "ITM003",
                                                 1, 58.0, 58.0);
        String str = ti.toString();

        assertTrue(str.contains("TXN999"));
        assertTrue(str.contains("ITM003"));
        assertTrue(str.contains("58.0"));
    }
}

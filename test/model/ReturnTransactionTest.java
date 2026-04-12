package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ReturnTransaction} model class.
 */
@DisplayName("ReturnTransaction Model Tests")
class ReturnTransactionTest {

    @Test
    @DisplayName("No-arg constructor creates ReturnTransaction with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        ReturnTransaction rt = new ReturnTransaction();

        assertEquals(0, rt.getReturnId());
        assertNull(rt.getTransactionId());
        assertNull(rt.getItemCode());
        assertEquals(0, rt.getQuantity());
        assertEquals(0.0, rt.getRefundAmount());
        assertNull(rt.getReturnDate());
        assertNull(rt.getProcessedBy());
        assertNull(rt.getReason());
    }

    @Test
    @DisplayName("Parameterised constructor sets transaction-related fields")
    void parameterizedConstructor_setsFields() {
        ReturnTransaction rt = new ReturnTransaction("TXN001", "ITM001", 2,
                                                     599.98, "SALES001", "Defective");

        assertEquals("TXN001", rt.getTransactionId());
        assertEquals("ITM001", rt.getItemCode());
        assertEquals(2, rt.getQuantity());
        assertEquals(599.98, rt.getRefundAmount(), 0.001);
        assertEquals("SALES001", rt.getProcessedBy());
        assertEquals("Defective", rt.getReason());
        // returnId and returnDate are not set by this constructor
        assertEquals(0, rt.getReturnId());
        assertNull(rt.getReturnDate());
    }

    @Test
    @DisplayName("ReturnDate can be set independently")
    void returnDate_canBeSet() {
        ReturnTransaction rt = new ReturnTransaction();
        LocalDateTime now = LocalDateTime.now();
        rt.setReturnDate(now);
        assertEquals(now, rt.getReturnDate());
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        ReturnTransaction rt = new ReturnTransaction("TXN555", "ITM003", 1,
                                                     45.0, "SALES002", "Wrong item");
        String str = rt.toString();

        assertTrue(str.contains("TXN555"));
        assertTrue(str.contains("ITM003"));
        assertTrue(str.contains("45.0"));
        assertTrue(str.contains("Wrong item"));
    }
}

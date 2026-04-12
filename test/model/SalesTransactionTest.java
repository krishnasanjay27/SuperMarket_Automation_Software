package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SalesTransaction} model class.
 */
@DisplayName("SalesTransaction Model Tests")
class SalesTransactionTest {

    @Test
    @DisplayName("No-arg constructor creates SalesTransaction with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        SalesTransaction txn = new SalesTransaction();

        assertNull(txn.getTransactionId());
        assertNull(txn.getTransactionDate());
        assertEquals(0.0, txn.getTotalAmount());
        assertNull(txn.getStatus());
        assertNull(txn.getSalesStaffId());
        assertNull(txn.getCustomerId());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        SalesTransaction txn = new SalesTransaction("TXN001", now, 792.98,
                                                    "FINALIZED", "SALES001", 1);

        assertEquals("TXN001", txn.getTransactionId());
        assertEquals(now, txn.getTransactionDate());
        assertEquals(792.98, txn.getTotalAmount(), 0.001);
        assertEquals("FINALIZED", txn.getStatus());
        assertEquals("SALES001", txn.getSalesStaffId());
        assertEquals(1, txn.getCustomerId());
    }

    @Test
    @DisplayName("CustomerId can be null (no customer linked)")
    void customerId_canBeNull() {
        SalesTransaction txn = new SalesTransaction();
        txn.setCustomerId(5);
        assertEquals(5, txn.getCustomerId());
        txn.setCustomerId(null);
        assertNull(txn.getCustomerId());
    }

    @Test
    @DisplayName("Status values can be ACTIVE, FINALIZED, or ABORTED")
    void status_acceptsAllValues() {
        SalesTransaction txn = new SalesTransaction();
        for (String status : new String[]{"ACTIVE", "FINALIZED", "ABORTED"}) {
            txn.setStatus(status);
            assertEquals(status, txn.getStatus());
        }
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        SalesTransaction txn = new SalesTransaction("TXN123", LocalDateTime.now(),
                                                    100.0, "ACTIVE", "SALES002", null);
        String str = txn.toString();

        assertTrue(str.contains("TXN123"));
        assertTrue(str.contains("ACTIVE"));
        assertTrue(str.contains("SALES002"));
    }
}

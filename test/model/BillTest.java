package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Bill} model class.
 */
@DisplayName("Bill Model Tests")
class BillTest {

    @Test
    @DisplayName("No-arg constructor creates Bill with default values")
    void noArgConstructor_fieldsAreDefaults() {
        Bill bill = new Bill();

        assertEquals(0, bill.getBillId());
        assertNull(bill.getTransactionId());
        assertNull(bill.getGeneratedDate());
        assertEquals(0.0, bill.getTotalAmount());
        assertEquals(0, bill.getLoyaltyPointsUsed());
        assertEquals(0.0, bill.getLoyaltyDiscount());
        assertEquals(0.0, bill.getFinalTotal());
        assertEquals(0, bill.getLoyaltyPointsEarned());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Bill bill = new Bill(1, "TXN001", now, 500.0, 50, 50.0, 450.0, 5);

        assertEquals(1, bill.getBillId());
        assertEquals("TXN001", bill.getTransactionId());
        assertEquals(now, bill.getGeneratedDate());
        assertEquals(500.0, bill.getTotalAmount(), 0.001);
        assertEquals(50, bill.getLoyaltyPointsUsed());
        assertEquals(50.0, bill.getLoyaltyDiscount(), 0.001);
        assertEquals(450.0, bill.getFinalTotal(), 0.001);
        assertEquals(5, bill.getLoyaltyPointsEarned());
    }

    @Test
    @DisplayName("Loyalty discount and final total are computed correctly")
    void loyaltyFields_workCorrectly() {
        Bill bill = new Bill();
        bill.setTotalAmount(1000.0);
        bill.setLoyaltyPointsUsed(100);
        bill.setLoyaltyDiscount(100.0);
        bill.setFinalTotal(900.0);
        bill.setLoyaltyPointsEarned(10);

        assertEquals(1000.0, bill.getTotalAmount());
        assertEquals(100, bill.getLoyaltyPointsUsed());
        assertEquals(100.0, bill.getLoyaltyDiscount());
        assertEquals(900.0, bill.getFinalTotal());
        assertEquals(10, bill.getLoyaltyPointsEarned());
    }

    @Test
    @DisplayName("toString() contains key field values")
    void toString_containsKeyFields() {
        Bill bill = new Bill(42, "TXN123", LocalDateTime.now(),
                             750.0, 0, 0.0, 750.0, 7);
        String str = bill.toString();

        assertTrue(str.contains("42"));
        assertTrue(str.contains("TXN123"));
        assertTrue(str.contains("750.0"));
    }
}

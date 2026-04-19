package service;

import dao.BillDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Bill;
import model.SalesTransaction;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BillService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillService Tests")
class BillServiceTest {

    @Mock private BillDAO             billDAO;
    @Mock private SalesTransactionDAO txnDAO;
    @Mock private TransactionItemDAO  txnItemDAO;

    private BillService billService;

    @BeforeEach
    void setUp() {
        billService = new BillService(billDAO, txnDAO, txnItemDAO);
    }

    // ─── generateBill() ───────────────────────────────────────────

    @Nested
    @DisplayName("generateBill()")
    class GenerateBillTests {

        @Test
        @DisplayName("Returns existing bill if already generated")
        void generateBill_existingBill_returnsExisting() {
            Bill existing = new Bill();
            existing.setBillId(42);
            existing.setTransactionId("TXN001");

            when(billDAO.getBillByTransactionId("TXN001")).thenReturn(existing);

            Bill result = billService.generateBill("TXN001");

            assertNotNull(result);
            assertEquals(42, result.getBillId());
            verify(billDAO, never()).generateBill(any());
        }

        @Test
        @DisplayName("Returns null when transaction not found")
        void generateBill_txnNotFound_returnsNull() {
            when(billDAO.getBillByTransactionId("NOPE")).thenReturn(null);
            when(txnDAO.getTransactionById("NOPE")).thenReturn(null);

            assertNull(billService.generateBill("NOPE"));
        }

        @Test
        @DisplayName("Returns null when transaction is not FINALIZED")
        void generateBill_txnNotFinalized_returnsNull() {
            when(billDAO.getBillByTransactionId("TXN001")).thenReturn(null);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    100.0, "ACTIVE", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertNull(billService.generateBill("TXN001"));
        }

        @Test
        @DisplayName("Generates and returns new bill for FINALIZED transaction")
        void generateBill_success() {
            when(billDAO.getBillByTransactionId("TXN001")).thenReturn(null);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    792.98, "FINALIZED", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(billDAO.generateBill(any(Bill.class))).thenReturn(true);

            Bill result = billService.generateBill("TXN001");

            assertNotNull(result);
            assertEquals("TXN001", result.getTransactionId());
            assertEquals(792.98, result.getTotalAmount(), 0.001);
            verify(billDAO).generateBill(any(Bill.class));
        }

        @Test
        @DisplayName("Returns null when DAO save fails")
        void generateBill_saveFails_returnsNull() {
            when(billDAO.getBillByTransactionId("TXN001")).thenReturn(null);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    100.0, "FINALIZED", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(billDAO.generateBill(any())).thenReturn(false);

            assertNull(billService.generateBill("TXN001"));
        }
    }

    // ─── getBillByTransactionId() ─────────────────────────────────

    @Test
    @DisplayName("getBillByTransactionId() delegates to DAO")
    void getBillByTransactionId_delegatesToDAO() {
        Bill bill = new Bill();
        bill.setBillId(1);
        when(billDAO.getBillByTransactionId("TXN001")).thenReturn(bill);

        Bill result = billService.getBillByTransactionId("TXN001");

        assertNotNull(result);
        assertEquals(1, result.getBillId());
    }
}

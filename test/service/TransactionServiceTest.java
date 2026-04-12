package service;

import dao.BillDAO;
import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Item;
import model.SalesTransaction;
import model.TransactionItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock private SalesTransactionDAO txnDAO;
    @Mock private TransactionItemDAO  txnItemDAO;
    @Mock private InventoryRecordDAO  inventoryDAO;
    @Mock private ItemDAO             itemDAO;
    @Mock private BillDAO             billDAO;
    @Mock private CustomerService     customerService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                txnDAO, txnItemDAO, inventoryDAO, itemDAO, billDAO, customerService);
    }

    // ─── createTransaction() ──────────────────────────────────────

    @Nested
    @DisplayName("createTransaction()")
    class CreateTransactionTests {

        @Test
        @DisplayName("Returns null when salesStaffId is null")
        void createTransaction_nullStaffId_returnsNull() {
            assertNull(transactionService.createTransaction(null));
        }

        @Test
        @DisplayName("Returns null when salesStaffId is blank")
        void createTransaction_blankStaffId_returnsNull() {
            assertNull(transactionService.createTransaction("  "));
        }

        @Test
        @DisplayName("Returns transaction ID on success")
        void createTransaction_success_returnsId() {
            when(txnDAO.createTransaction(any(SalesTransaction.class))).thenReturn(true);

            String txnId = transactionService.createTransaction("SALES001");

            assertNotNull(txnId);
            assertTrue(txnId.startsWith("TXN"));
            verify(txnDAO).createTransaction(any(SalesTransaction.class));
        }

        @Test
        @DisplayName("Returns null when DAO create fails")
        void createTransaction_daoFails_returnsNull() {
            when(txnDAO.createTransaction(any(SalesTransaction.class))).thenReturn(false);

            assertNull(transactionService.createTransaction("SALES001"));
        }
    }

    // ─── addItemToTransaction() ───────────────────────────────────

    @Nested
    @DisplayName("addItemToTransaction()")
    class AddItemToTransactionTests {

        @Test
        @DisplayName("Returns false when quantity is zero or negative")
        void addItem_invalidQuantity_returnsFalse() {
            assertFalse(transactionService.addItemToTransaction("TXN001", "ITM001", 0));
            assertFalse(transactionService.addItemToTransaction("TXN001", "ITM001", -1));
        }

        @Test
        @DisplayName("Returns false when transaction not found")
        void addItem_txnNotFound_returnsFalse() {
            when(txnDAO.getTransactionById("TXN_NOPE")).thenReturn(null);
            assertFalse(transactionService.addItemToTransaction("TXN_NOPE", "ITM001", 1));
        }

        @Test
        @DisplayName("Returns false when transaction is not ACTIVE")
        void addItem_txnNotActive_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "FINALIZED", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertFalse(transactionService.addItemToTransaction("TXN001", "ITM001", 1));
        }

        @Test
        @DisplayName("Returns false when item not found")
        void addItem_itemNotFound_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(itemDAO.getItemByCode("ITM_NOPE")).thenReturn(null);

            assertFalse(transactionService.addItemToTransaction("TXN001", "ITM_NOPE", 1));
        }

        @Test
        @DisplayName("Returns false when insufficient stock")
        void addItem_insufficientStock_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);

            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(2);

            assertFalse(transactionService.addItemToTransaction("TXN001", "ITM001", 5));
        }

        @Test
        @DisplayName("Adds item to transaction on success")
        void addItem_success_delegatesToDAO() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);

            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(100);
            when(txnItemDAO.addTransactionItem(any(TransactionItem.class))).thenReturn(true);

            assertTrue(transactionService.addItemToTransaction("TXN001", "ITM001", 3));

            verify(txnItemDAO).addTransactionItem(argThat(ti ->
                    ti.getItemCode().equals("ITM001")
                    && ti.getQuantity() == 3
                    && ti.getUnitPrice() == 299.99
                    && Math.abs(ti.getLineTotal() - 899.97) < 0.01));
        }
    }

    // ─── updateItemQuantity() ─────────────────────────────────────

    @Nested
    @DisplayName("updateItemQuantity()")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Returns false when quantity is zero or negative")
        void updateQuantity_invalidQty_returnsFalse() {
            assertFalse(transactionService.updateItemQuantity(1, 0));
            assertFalse(transactionService.updateItemQuantity(1, -5));
        }

        @Test
        @DisplayName("Returns false when TransactionItem not found")
        void updateQuantity_notFound_returnsFalse() {
            when(txnItemDAO.getTransactionItemById(999)).thenReturn(null);
            assertFalse(transactionService.updateItemQuantity(999, 5));
        }

        @Test
        @DisplayName("Returns false when transaction is not ACTIVE")
        void updateQuantity_txnNotActive_returnsFalse() {
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    200.0, "FINALIZED", "SALES001", null);

            when(txnItemDAO.getTransactionItemById(1)).thenReturn(ti);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertFalse(transactionService.updateItemQuantity(1, 3));
        }

        @Test
        @DisplayName("Returns false when insufficient stock for new quantity")
        void updateQuantity_insufficientStock_returnsFalse() {
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    200.0, "ACTIVE", "SALES001", null);

            when(txnItemDAO.getTransactionItemById(1)).thenReturn(ti);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(3);

            assertFalse(transactionService.updateItemQuantity(1, 10));
        }

        @Test
        @DisplayName("Updates quantity on success")
        void updateQuantity_success() {
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    200.0, "ACTIVE", "SALES001", null);

            when(txnItemDAO.getTransactionItemById(1)).thenReturn(ti);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(50);
            when(txnItemDAO.updateTransactionItemQuantity(1, 5)).thenReturn(true);

            assertTrue(transactionService.updateItemQuantity(1, 5));
        }
    }

    // ─── removeItemFromTransaction() ──────────────────────────────

    @Nested
    @DisplayName("removeItemFromTransaction()")
    class RemoveItemTests {

        @Test
        @DisplayName("Returns false when item not found")
        void removeItem_notFound_returnsFalse() {
            when(txnItemDAO.getTransactionItemById(999)).thenReturn(null);
            assertFalse(transactionService.removeItemFromTransaction(999));
        }

        @Test
        @DisplayName("Returns false when transaction is not ACTIVE")
        void removeItem_txnNotActive_returnsFalse() {
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    200.0, "ABORTED", "SALES001", null);

            when(txnItemDAO.getTransactionItemById(1)).thenReturn(ti);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertFalse(transactionService.removeItemFromTransaction(1));
        }

        @Test
        @DisplayName("Removes item on success")
        void removeItem_success() {
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    200.0, "ACTIVE", "SALES001", null);

            when(txnItemDAO.getTransactionItemById(1)).thenReturn(ti);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.removeTransactionItem(1)).thenReturn(true);

            assertTrue(transactionService.removeItemFromTransaction(1));
        }
    }

    // ─── finalizeTransaction() ────────────────────────────────────

    @Nested
    @DisplayName("finalizeTransaction()")
    class FinalizeTests {

        @Test
        @DisplayName("Returns false when transaction not found")
        void finalize_txnNotFound_returnsFalse() {
            when(txnDAO.getTransactionById("NOPE")).thenReturn(null);
            assertFalse(transactionService.finalizeTransaction("NOPE"));
        }

        @Test
        @DisplayName("Returns false when transaction is not ACTIVE")
        void finalize_txnNotActive_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    100.0, "FINALIZED", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertFalse(transactionService.finalizeTransaction("TXN001"));
        }

        @Test
        @DisplayName("Returns false when transaction has no items")
        void finalize_noItems_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.getItemsByTransactionId("TXN001")).thenReturn(Collections.emptyList());

            assertFalse(transactionService.finalizeTransaction("TXN001"));
        }

        @Test
        @DisplayName("Returns false when pointsToRedeem is negative")
        void finalize_negativePoints_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 1, 100.0, 100.0);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.getItemsByTransactionId("TXN001")).thenReturn(List.of(ti));

            assertFalse(transactionService.finalizeTransaction("TXN001", 1, -5));
        }

        @Test
        @DisplayName("Finalizes transaction without customer (backward compat)")
        void finalize_noCustomer_success() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            TransactionItem ti1 = new TransactionItem(1, "TXN001", "ITM001", 2, 100.0, 200.0);
            TransactionItem ti2 = new TransactionItem(2, "TXN001", "ITM002", 1, 50.0, 50.0);

            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.getItemsByTransactionId("TXN001")).thenReturn(Arrays.asList(ti1, ti2));
            when(txnDAO.finalizeTransaction("TXN001")).thenReturn(true);
            when(customerService.calculateEarnedPoints(250.0)).thenReturn(2);
            when(billDAO.generateBill(any())).thenReturn(true);

            assertTrue(transactionService.finalizeTransaction("TXN001"));

            verify(txnDAO).finalizeTransaction("TXN001");
            verify(txnDAO, never()).setCustomerId(anyString(), anyInt());
            verify(billDAO).generateBill(any());
        }

        @Test
        @DisplayName("Finalizes with customer and loyalty points")
        void finalize_withCustomer_success() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 5, 200.0, 1000.0);

            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.getItemsByTransactionId("TXN001")).thenReturn(List.of(ti));
            when(customerService.hasEnoughPoints(1, 100)).thenReturn(true);
            when(customerService.calculateDiscount(100)).thenReturn(100.0);
            when(txnDAO.finalizeTransaction("TXN001")).thenReturn(true);
            when(customerService.calculateEarnedPoints(1000.0)).thenReturn(10);
            when(billDAO.generateBill(any())).thenReturn(true);

            assertTrue(transactionService.finalizeTransaction("TXN001", 1, 100));

            verify(txnDAO).setCustomerId("TXN001", 1);
            verify(customerService).redeemPoints(1, 100);
            verify(customerService).addLoyaltyPoints(1, 10);
        }

        @Test
        @DisplayName("Discount is capped at subtotal to prevent negative final total")
        void finalize_discountCappedAtSubtotal() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    0, "ACTIVE", "SALES001", null);
            TransactionItem ti = new TransactionItem(1, "TXN001", "ITM001", 1, 50.0, 50.0);

            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnItemDAO.getItemsByTransactionId("TXN001")).thenReturn(List.of(ti));
            when(customerService.hasEnoughPoints(1, 200)).thenReturn(true);
            when(customerService.calculateDiscount(200)).thenReturn(200.0);  // > subtotal of 50
            when(txnDAO.finalizeTransaction("TXN001")).thenReturn(true);
            when(customerService.calculateEarnedPoints(50.0)).thenReturn(0);
            when(billDAO.generateBill(any())).thenReturn(true);

            assertTrue(transactionService.finalizeTransaction("TXN001", 1, 200));

            // Verify bill has discount capped at subtotal
            verify(billDAO).generateBill(argThat(bill ->
                    bill.getLoyaltyDiscount() == 50.0 && bill.getFinalTotal() == 0.0));
        }
    }

    // ─── abortTransaction() ───────────────────────────────────────

    @Nested
    @DisplayName("abortTransaction()")
    class AbortTests {

        @Test
        @DisplayName("Returns false when transaction not found")
        void abort_notFound_returnsFalse() {
            when(txnDAO.getTransactionById("NOPE")).thenReturn(null);
            assertFalse(transactionService.abortTransaction("NOPE"));
        }

        @Test
        @DisplayName("Returns false when transaction is not ACTIVE")
        void abort_notActive_returnsFalse() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    100.0, "FINALIZED", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);

            assertFalse(transactionService.abortTransaction("TXN001"));
        }

        @Test
        @DisplayName("Aborts transaction on success")
        void abort_success() {
            SalesTransaction txn = new SalesTransaction("TXN001", LocalDateTime.now(),
                    100.0, "ACTIVE", "SALES001", null);
            when(txnDAO.getTransactionById("TXN001")).thenReturn(txn);
            when(txnDAO.abortTransaction("TXN001")).thenReturn(true);

            assertTrue(transactionService.abortTransaction("TXN001"));
        }
    }
}

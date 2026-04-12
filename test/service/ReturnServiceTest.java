package service;

import dao.ReturnDAO;
import dao.SalesTransactionDAO;
import model.SalesTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReturnService Tests")
class ReturnServiceTest {

    @Mock private ReturnDAO returnDAO;
    @Mock private SalesTransactionDAO salesTransactionDAO;
    @Mock private CustomerService customerService;

    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        returnService = new ReturnService(returnDAO, salesTransactionDAO, customerService);
    }

    @Test
    @DisplayName("validateTransaction() returns null for blank transactionId")
    void validateTransaction_blankId_returnsNull() {
        assertNull(returnService.validateTransaction(""));
    }

    @Test
    @DisplayName("validateTransaction() returns null when transaction not found")
    void validateTransaction_notFound_returnsNull() {
        when(salesTransactionDAO.getTransactionById("TXN1")).thenReturn(null);
        assertNull(returnService.validateTransaction("TXN1"));
    }

    @Test
    @DisplayName("validateTransaction() returns null for non-FINALIZED transaction")
    void validateTransaction_notFinalized_returnsNull() {
        SalesTransaction txn = new SalesTransaction();
        txn.setStatus("ACTIVE");
        when(salesTransactionDAO.getTransactionById("TXN1")).thenReturn(txn);
        assertNull(returnService.validateTransaction("TXN1"));
    }

    @Test
    @DisplayName("validateTransaction() returns txn for FINALIZED transaction")
    void validateTransaction_finalized_returnsTxn() {
        SalesTransaction txn = new SalesTransaction();
        txn.setStatus("FINALIZED");
        when(salesTransactionDAO.getTransactionById("TXN1")).thenReturn(txn);
        assertNotNull(returnService.validateTransaction("TXN1"));
    }

    @Test
    @DisplayName("getReturnableItems() delegates to DAO")
    void getReturnableItems_delegates() {
        when(returnDAO.getTransactionItems("TXN1")).thenReturn(Collections.emptyList());
        assertTrue(returnService.getReturnableItems("TXN1").isEmpty());
        verify(returnDAO).getTransactionItems("TXN1");
    }

    @Test
    @DisplayName("getRemainingReturnableQuantity() delegates to DAO")
    void getRemainingReturnableQuantity_delegates() {
        when(returnDAO.getRemainingReturnableQuantity("TXN1", "ITM1")).thenReturn(5);
        assertEquals(5, returnService.getRemainingReturnableQuantity("TXN1", "ITM1"));
    }

    @Test
    @DisplayName("calculateRefundAmount() calculates correctly")
    void calculateRefundAmount_calculates() {
        when(returnDAO.getOriginalUnitPrice("TXN1", "ITM1")).thenReturn(100.0);
        assertEquals(300.0, returnService.calculateRefundAmount("TXN1", "ITM1", 3));
    }

    @Test
    @DisplayName("processReturn() fails validation - bad txn")
    void processReturn_badTxn_returnsFalse() {
        when(salesTransactionDAO.getTransactionById("TXN1")).thenReturn(null);
        assertFalse(returnService.processReturn("TXN1", "ITM1", 1, "P1", "Reason"));
    }

    // A full processReturn mock sequence requires a static mock on DBConnection
    // which is hard in standard Mockito 5 without mock-maker inline for static methods.
    // Testing failure due to missing eligibility:
    @Test
    @DisplayName("processReturn() fails validation - not eligible")
    void processReturn_notEligible_returnsFalse() {
        SalesTransaction txn = new SalesTransaction();
        txn.setStatus("FINALIZED");
        when(salesTransactionDAO.getTransactionById("TXN1")).thenReturn(txn);
        when(returnDAO.getTransactionItems("TXN1")).thenReturn(Collections.emptyList());
        
        assertFalse(returnService.processReturn("TXN1", "ITM1", 1, "P1", "Reason"));
    }
}

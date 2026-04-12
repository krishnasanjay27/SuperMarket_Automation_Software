package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.PriceHistoryDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.InventoryRecord;
import model.InventoryVendorStatus;
import model.LowStockVendorAlert;
import model.PriceHistory;
import model.SalesTransaction;
import model.TransactionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Tests")
class ReportServiceTest {

    @Mock private SalesTransactionDAO txnDAO;
    @Mock private TransactionItemDAO txnItemDAO;
    @Mock private ItemDAO itemDAO;
    @Mock private InventoryRecordDAO inventoryDAO;
    @Mock private PriceHistoryDAO priceHistoryDAO;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(txnDAO, txnItemDAO, itemDAO, inventoryDAO, priceHistoryDAO);
    }

    @Test
    @DisplayName("getTransactionsByDateRange() returns empty list for null dates")
    void getTransactionsByDateRange_nullDates_returnsEmpty() {
        assertTrue(reportService.getTransactionsByDateRange(null, LocalDateTime.now()).isEmpty());
        assertTrue(reportService.getTransactionsByDateRange(LocalDateTime.now(), null).isEmpty());
    }

    @Test
    @DisplayName("getTransactionsByDateRange() returns empty list when end is before start")
    void getTransactionsByDateRange_endBeforeStart_returnsEmpty() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1);
        assertTrue(reportService.getTransactionsByDateRange(start, end).isEmpty());
    }

    @Test
    @DisplayName("getTransactionsByDateRange() delegates to DAO")
    void getTransactionsByDateRange_validDates_delegates() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(txnDAO.getTransactionsByDateRange(start, end)).thenReturn(List.of(new SalesTransaction()));
        
        assertFalse(reportService.getTransactionsByDateRange(start, end).isEmpty());
        verify(txnDAO).getTransactionsByDateRange(start, end);
    }

    @Test
    @DisplayName("getTransactionsByStaff() returns empty for blank staffId")
    void getTransactionsByStaff_blankStaff_returnsEmpty() {
        assertTrue(reportService.getTransactionsByStaff(" ").isEmpty());
    }

    @Test
    @DisplayName("getTransactionsByStaff() delegates to DAO")
    void getTransactionsByStaff_validStaff_delegates() {
        when(txnDAO.getTransactionsByStaff("S01")).thenReturn(List.of(new SalesTransaction()));
        assertFalse(reportService.getTransactionsByStaff("S01").isEmpty());
    }

    @Test
    @DisplayName("getLowStockItemsWithVendor() delegates to DAO")
    void getLowStockItemsWithVendor_delegates() {
        when(itemDAO.getLowStockItemsWithVendor()).thenReturn(List.of(new LowStockVendorAlert()));
        assertFalse(reportService.getLowStockItemsWithVendor().isEmpty());
    }

    @Test
    @DisplayName("getInventoryStatus() delegates to DAO")
    void getInventoryStatus_delegates() {
        when(inventoryDAO.getAllInventoryRecords()).thenReturn(List.of(new InventoryRecord()));
        assertFalse(reportService.getInventoryStatus().isEmpty());
    }

    @Test
    @DisplayName("getInventoryStatusWithVendor() delegates to DAO")
    void getInventoryStatusWithVendor_delegates() {
        when(inventoryDAO.getInventoryStatusWithVendor()).thenReturn(List.of(new InventoryVendorStatus()));
        assertFalse(reportService.getInventoryStatusWithVendor().isEmpty());
    }

    @Test
    @DisplayName("getPriceHistory() returns empty for blank itemCode")
    void getPriceHistory_blankCode_returnsEmpty() {
        assertTrue(reportService.getPriceHistory("").isEmpty());
    }

    @Test
    @DisplayName("getPriceHistory() returns empty for non-existent item")
    void getPriceHistory_notFound_returnsEmpty() {
        when(itemDAO.getItemByCode("NOPE")).thenReturn(null);
        assertTrue(reportService.getPriceHistory("NOPE").isEmpty());
    }

    @Test
    @DisplayName("getPriceHistory() delegates to DAO for existing item")
    void getPriceHistory_validItem_delegates() {
        when(itemDAO.getItemByCode("ITM1")).thenReturn(new model.Item());
        when(priceHistoryDAO.getPriceHistoryByItem("ITM1")).thenReturn(List.of(new PriceHistory()));
        assertFalse(reportService.getPriceHistory("ITM1").isEmpty());
    }

    @Test
    @DisplayName("getSalesLineItemsByTransaction() returns empty for blank ID")
    void getSalesLineItemsByTransaction_blankId_returnsEmpty() {
        assertTrue(reportService.getSalesLineItemsByTransaction("").isEmpty());
    }

    @Test
    @DisplayName("getSalesLineItemsByTransaction() delegates to DAO")
    void getSalesLineItemsByTransaction_validId_delegates() {
        when(txnItemDAO.getItemsByTransactionId("TXN1")).thenReturn(List.of(new TransactionItem()));
        assertFalse(reportService.getSalesLineItemsByTransaction("TXN1").isEmpty());
    }
}

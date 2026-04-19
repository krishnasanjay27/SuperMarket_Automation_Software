package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.PriceHistoryDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.InventoryRecord;
import model.Item;
import model.PriceHistory;
import model.SalesTransaction;
import model.TransactionItem;

import java.time.LocalDateTime;

import java.util.Collections;
import java.util.List;

public class ReportService {

    private final SalesTransactionDAO txnDAO;
    private final TransactionItemDAO  txnItemDAO;
    private final ItemDAO             itemDAO;
    private final InventoryRecordDAO  inventoryDAO;
    private final PriceHistoryDAO     priceHistoryDAO;

    public ReportService() {
        this.txnDAO          = new SalesTransactionDAO();
        this.txnItemDAO      = new TransactionItemDAO();
        this.itemDAO         = new ItemDAO();
        this.inventoryDAO    = new InventoryRecordDAO();
        this.priceHistoryDAO = new PriceHistoryDAO();
    }

    /** Constructor for dependency injection (testing). */
    public ReportService(SalesTransactionDAO txnDAO, TransactionItemDAO txnItemDAO,
                         ItemDAO itemDAO, InventoryRecordDAO inventoryDAO,
                         PriceHistoryDAO priceHistoryDAO) {
        this.txnDAO          = txnDAO;
        this.txnItemDAO      = txnItemDAO;
        this.itemDAO         = itemDAO;
        this.inventoryDAO    = inventoryDAO;
        this.priceHistoryDAO = priceHistoryDAO;
    }

    public List<SalesTransaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            System.err.println("getTransactionsByDateRange() failed – start and end must not be null.");
            return Collections.emptyList();
        }
        if (end.isBefore(start)) {
            System.err.println("getTransactionsByDateRange() failed – end date is before start date.");
            return Collections.emptyList();
        }
        return txnDAO.getTransactionsByDateRange(start, end);
    }

    public List<SalesTransaction> getTransactionsByStaff(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            System.err.println("getTransactionsByStaff() failed – staffId must not be empty.");
            return Collections.emptyList();
        }
        return txnDAO.getTransactionsByStaff(staffId);
    }

    public List<model.LowStockVendorAlert> getLowStockItemsWithVendor() {
        List<model.LowStockVendorAlert> alerts = itemDAO.getLowStockItemsWithVendor();
        if (alerts.isEmpty()) {
            System.out.println("getLowStockItemsWithVendor() – no items below reorder level.");
        }
        return alerts;
    }

    public List<InventoryRecord> getInventoryStatus() {
        List<InventoryRecord> records = inventoryDAO.getAllInventoryRecords();

        if (records.isEmpty()) {
            System.out.println("getInventoryStatus() – inventory table is empty.");
        }

        return records;
    }

    public List<model.InventoryVendorStatus> getInventoryStatusWithVendor() {
        List<model.InventoryVendorStatus> records = inventoryDAO.getInventoryStatusWithVendor();
        if (records.isEmpty()) {
            System.out.println("getInventoryStatusWithVendor() – inventory table is empty.");
        }
        return records;
    }

    public List<PriceHistory> getPriceHistory(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            System.err.println("getPriceHistory() failed – itemCode must not be empty.");
            return Collections.emptyList();
        }

        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("getPriceHistory() – no item found with code: " + itemCode);
            return Collections.emptyList();
        }

        return priceHistoryDAO.getPriceHistoryByItem(itemCode);
    }

    public List<TransactionItem> getSalesLineItemsByTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            System.err.println("getSalesLineItemsByTransaction() failed – transactionId must not be empty.");
            return Collections.emptyList();
        }
        return txnItemDAO.getItemsByTransactionId(transactionId);
    }
}

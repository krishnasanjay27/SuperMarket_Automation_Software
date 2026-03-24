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
import java.util.ArrayList;
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

    public List<Item> getItemsBelowReorderLevel() {
        List<Item> lowStockItems = new ArrayList<>();

        List<Item> allItems = itemDAO.getAllItems();
        if (allItems.isEmpty()) {
            System.out.println("getItemsBelowReorderLevel() – no items found in catalogue.");
            return lowStockItems;
        }

        for (Item item : allItems) {
            int stock = inventoryDAO.getStockLevel(item.getItemCode());
            int effectiveStock = (stock < 0) ? 0 : stock;

            if (effectiveStock <= item.getReorderLevel()) {
                lowStockItems.add(item);
                System.out.println("Low stock: '" + item.getItemCode()
                                   + "' – Stock: " + effectiveStock
                                   + ", Reorder Level: " + item.getReorderLevel());
            }
        }

        return lowStockItems;
    }

    public List<InventoryRecord> getInventoryStatus() {
        List<InventoryRecord> records = inventoryDAO.getAllInventoryRecords();

        if (records.isEmpty()) {
            System.out.println("getInventoryStatus() – inventory table is empty.");
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

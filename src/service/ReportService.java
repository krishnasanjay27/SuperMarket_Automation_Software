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

/**
 * ReportService – read-only reporting layer for manager dashboards.
 *
 * Aggregates data from five DAOs; performs no writes.
 * Contains no SQL and no UI logic.
 */
public class ReportService {

    // ----------------------------------------------------------------
    // DAO dependencies
    // ----------------------------------------------------------------
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

    // ================================================================
    // 1. getTransactionsByDateRange(LocalDateTime start, LocalDateTime end)
    // ================================================================

    /**
     * Returns all sales transactions whose {@code transactionDate} falls
     * within the specified range (inclusive).
     *
     * @param start range start; must not be null
     * @param end   range end; must not be null and must be after start
     * @return list of matching {@link SalesTransaction} objects, newest first;
     *         empty list on invalid input or no matches
     */
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

    // ================================================================
    // 2. getTransactionsByStaff(String staffId)
    // ================================================================

    /**
     * Returns all transactions created by a specific sales staff member,
     * ordered most-recent first.
     *
     * @param staffId the userId of the sales staff member
     * @return list of {@link SalesTransaction} objects; empty if none found
     */
    public List<SalesTransaction> getTransactionsByStaff(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            System.err.println("getTransactionsByStaff() failed – staffId must not be empty.");
            return Collections.emptyList();
        }

        return txnDAO.getTransactionsByStaff(staffId);
    }

    // ================================================================
    // 3. getItemsBelowReorderLevel()
    // ================================================================

    /**
     * Returns all items whose current stock level is at or below their
     * configured reorder level — the low-stock alert report.
     *
     * <p>Implemented at the service layer by joining Item data with
     * InventoryRecord data in memory, avoiding a DAO-level SQL JOIN.</p>
     *
     * @return list of {@link Item} objects that need restocking;
     *         empty if all items are sufficiently stocked
     */
    public List<Item> getItemsBelowReorderLevel() {
        List<Item> lowStockItems = new ArrayList<>();

        List<Item> allItems = itemDAO.getAllItems();
        if (allItems.isEmpty()) {
            System.out.println("getItemsBelowReorderLevel() – no items found in catalogue.");
            return lowStockItems;
        }

        for (Item item : allItems) {
            int stock = inventoryDAO.getStockLevel(item.getItemCode());
            // stock == -1 means no inventory record at all → treat as 0 stock
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

    // ================================================================
    // 4. getInventoryStatus()
    // ================================================================

    /**
     * Returns a complete snapshot of all inventory records, useful for
     * generating a full inventory report.
     *
     * <p>Each {@link InventoryRecord} in the result carries the live
     * {@code stockLevel}, {@code lastUpdated}, and {@code updatedBy} values
     * as stored in the database.</p>
     *
     * @return list of all {@link InventoryRecord} objects, ordered by itemCode;
     *         empty list if no records exist
     */
    public List<InventoryRecord> getInventoryStatus() {
        List<InventoryRecord> records = inventoryDAO.getAllInventoryRecords();

        if (records.isEmpty()) {
            System.out.println("getInventoryStatus() – inventory table is empty.");
        }

        return records;
    }

    // ================================================================
    // 5. getPriceHistory(String itemCode)
    // ================================================================

    /**
     * Returns the complete price-change audit trail for a single item,
     * most recent change first.
     *
     * @param itemCode the item code to query
     * @return list of {@link PriceHistory} records; empty if none recorded
     */
    public List<PriceHistory> getPriceHistory(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            System.err.println("getPriceHistory() failed – itemCode must not be empty.");
            return Collections.emptyList();
        }

        // Confirm the item exists before querying history.
        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("getPriceHistory() – no item found with code: " + itemCode);
            return Collections.emptyList();
        }

        return priceHistoryDAO.getPriceHistoryByItem(itemCode);
    }

    // ================================================================
    // 6. getSalesLineItemsByTransaction(String transactionId)
    //    Convenience method: returns line items for a given transaction
    //    to support bill-detail and per-transaction drill-down reports.
    // ================================================================

    /**
     * Returns all line items for a specific transaction.
     * Useful for generating itemised bill reports.
     *
     * @param transactionId the transaction to drill into
     * @return list of {@link TransactionItem} objects; empty if none found
     */
    public List<TransactionItem> getSalesLineItemsByTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            System.err.println("getSalesLineItemsByTransaction() failed – transactionId must not be empty.");
            return Collections.emptyList();
        }

        return txnItemDAO.getItemsByTransactionId(transactionId);
    }
}

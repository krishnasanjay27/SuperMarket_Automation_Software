package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.PriceHistoryDAO;
import model.InventoryRecord;
import model.Item;
import model.PriceHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InventoryService – service layer for item and inventory management.
 *
 * Coordinates ItemDAO, InventoryRecordDAO, and PriceHistoryDAO.
 * Contains no SQL and no UI logic.
 *
 * Intended callers:
 *   - InventoryStaff  → updateStock, setStockLevel, getStockLevel
 *   - Manager         → addNewItem, updateItemPrice, all read methods
 */
public class InventoryService {

    // ----------------------------------------------------------------
    // DAO dependencies
    // ----------------------------------------------------------------
    private final ItemDAO            itemDAO;
    private final InventoryRecordDAO inventoryDAO;
    private final PriceHistoryDAO    priceHistoryDAO;

    public InventoryService() {
        this.itemDAO         = new ItemDAO();
        this.inventoryDAO    = new InventoryRecordDAO();
        this.priceHistoryDAO = new PriceHistoryDAO();
    }

    // ================================================================
    // 1. addNewItem(Item item, int initialStock, String updatedBy)
    // ================================================================

    /**
     * Adds a new product to the Item master catalogue and initialises its
     * inventory record with the specified stock quantity.
     *
     * <p>Both the Item row and the InventoryRecord row are inserted. If the
     * Item insert succeeds but the InventoryRecord insert fails, the item
     * exists in the catalogue with no stock row — this is safe because
     * {@link #getStockLevel(String)} returns -1 for missing inventory rows,
     * acting as a natural sentinel.</p>
     *
     * @param item         fully populated {@link Item}; itemCode must be unique
     * @param initialStock starting stock units (must be &ge; 0)
     * @param updatedBy    userId of the staff member performing the operation
     * @return {@code true} if both the item and its inventory record were created
     */
    public boolean addNewItem(Item item, int initialStock, String updatedBy) {
        if (item == null) {
            System.err.println("addNewItem() failed – item must not be null.");
            return false;
        }
        if (initialStock < 0) {
            System.err.println("addNewItem() failed – initialStock cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("addNewItem() failed – updatedBy must not be empty.");
            return false;
        }

        // --- Insert Item master row ---
        boolean itemAdded = itemDAO.addItem(item);
        if (!itemAdded) {
            System.err.println("addNewItem() failed – could not insert item: " + item.getItemCode());
            return false;
        }

        // --- Create the corresponding InventoryRecord ---
        InventoryRecord record = new InventoryRecord();
        record.setItemCode(item.getItemCode());
        record.setStockLevel(initialStock);
        record.setLastUpdated(LocalDateTime.now());
        record.setUpdatedBy(updatedBy);

        boolean inventoryCreated = inventoryDAO.addInventoryRecord(record);
        if (!inventoryCreated) {
            System.err.println("addNewItem() warning – item '" + item.getItemCode()
                               + "' added to catalogue but InventoryRecord creation failed.");
            return false;
        }

        System.out.println("New item '" + item.getItemCode() + "' added with initial stock: " + initialStock);
        return true;
    }

    // ================================================================
    // 2. updateItemPrice(String itemCode, double newPrice, String updatedBy)
    // ================================================================

    /**
     * Updates an item's selling price and records the change in PriceHistory.
     *
     * <p>Steps:
     * <ol>
     *   <li>Validates item exists.</li>
     *   <li>Validates newPrice &ge; 0.</li>
     *   <li>Records old → new price in {@code PriceHistory}.</li>
     *   <li>Updates {@code Item.price}.</li>
     * </ol>
     * The PriceHistory row is inserted first so the audit trail is never lost
     * even if the Item update partially fails.</p>
     *
     * @param itemCode  the item whose price is changing
     * @param newPrice  the replacement price (must be &ge; 0)
     * @param updatedBy userId of the Manager authorising the change
     * @return {@code true} if both the audit record and the price update succeeded
     */
    public boolean updateItemPrice(String itemCode, double newPrice, String updatedBy) {
        if (newPrice < 0) {
            System.err.println("updateItemPrice() failed – price cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("updateItemPrice() failed – updatedBy must not be empty.");
            return false;
        }

        // --- Confirm item exists and capture current price ---
        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("updateItemPrice() failed – item not found: " + itemCode);
            return false;
        }

        double oldPrice = item.getPrice();

        if (Double.compare(oldPrice, newPrice) == 0) {
            System.out.println("updateItemPrice() – new price is identical to current price. No change made.");
            return true; // idempotent success
        }

        // --- Audit log first ---
        PriceHistory audit = new PriceHistory();
        audit.setItemCode(itemCode);
        audit.setOldPrice(oldPrice);
        audit.setNewPrice(newPrice);
        audit.setChangedAt(LocalDateTime.now());
        audit.setChangedBy(updatedBy);

        boolean auditSaved = priceHistoryDAO.recordPriceChange(audit);
        if (!auditSaved) {
            System.err.println("updateItemPrice() failed – could not record price history for: " + itemCode);
            return false;
        }

        // --- Apply price change ---
        return itemDAO.updateItemPrice(itemCode, newPrice);
    }

    // ================================================================
    // 3. updateStock(String itemCode, int quantityChange, String updatedBy)
    // ================================================================

    /**
     * Increments or decrements stock by a delta amount.
     *
     * <p>Pass a positive value to add stock, a negative value to reduce it.
     * The DAO rejects any change that would push stock below zero.</p>
     *
     * @param itemCode       the item to adjust
     * @param quantityChange positive to add, negative to remove
     * @param updatedBy      userId of the staff member making the change
     * @return {@code true} if stock was updated successfully
     */
    public boolean updateStock(String itemCode, int quantityChange, String updatedBy) {
        if (quantityChange == 0) {
            System.err.println("updateStock() – quantityChange is 0, no update performed.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("updateStock() failed – updatedBy must not be empty.");
            return false;
        }

        // --- Validate item exists ---
        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("updateStock() failed – item not found: " + itemCode);
            return false;
        }

        // --- Verify stock won't go negative (pre-check for a clear error message) ---
        if (quantityChange < 0) {
            int currentStock = inventoryDAO.getStockLevel(itemCode);
            if (currentStock < 0) {
                System.err.println("updateStock() failed – no inventory record for item: " + itemCode);
                return false;
            }
            if ((currentStock + quantityChange) < 0) {
                System.err.println("updateStock() failed – insufficient stock for '" + itemCode
                                   + "'. Current: " + currentStock
                                   + ", Requested reduction: " + Math.abs(quantityChange));
                return false;
            }
        }

        return inventoryDAO.updateStock(itemCode, quantityChange, updatedBy);
    }

    // ================================================================
    // 4. setStockLevel(String itemCode, int newStock, String updatedBy)
    // ================================================================

    /**
     * Directly overwrites the stock level for an item (e.g., after a
     * physical stock count).
     *
     * @param itemCode   the item to correct
     * @param newStock   the verified physical stock count (must be &ge; 0)
     * @param updatedBy  userId of the staff member making the correction
     * @return {@code true} if the stock level was set successfully
     */
    public boolean setStockLevel(String itemCode, int newStock, String updatedBy) {
        if (newStock < 0) {
            System.err.println("setStockLevel() failed – stock level cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("setStockLevel() failed – updatedBy must not be empty.");
            return false;
        }

        // --- Validate item exists ---
        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("setStockLevel() failed – item not found: " + itemCode);
            return false;
        }

        return inventoryDAO.setStockLevel(itemCode, newStock, updatedBy);
    }

    // ================================================================
    // 5. getStockLevel(String itemCode)
    // ================================================================

    /**
     * Returns the current stock level for an item.
     *
     * @param itemCode the item to query
     * @return current stock level, or {@code -1} if the item has no inventory record
     */
    public int getStockLevel(String itemCode) {
        return inventoryDAO.getStockLevel(itemCode);
    }

    // ================================================================
    // 6. getItemByCode(String itemCode)
    // ================================================================

    /**
     * Retrieves a product from the Item master catalogue by its code.
     *
     * @param itemCode the item code to look up
     * @return the matching {@link Item}, or {@code null} if not found
     */
    public Item getItemByCode(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            System.err.println("getItemByCode() failed – itemCode must not be empty.");
            return null;
        }
        return itemDAO.getItemByCode(itemCode);
    }

    // ================================================================
    // 7. getAllItems()
    // ================================================================

    /**
     * Returns the full product catalogue, ordered by category then name.
     *
     * @return list of all {@link Item} records; empty list if none found
     */
    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}

package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Item;
import model.SalesTransaction;
import model.TransactionItem;

import java.time.LocalDateTime;

/**
 * TransactionService – business-logic layer for sales transactions.
 *
 * Coordinates between SalesTransactionDAO, TransactionItemDAO,
 * InventoryRecordDAO, and ItemDAO. Contains no SQL and no UI logic.
 *
 * Responsibilities:
 *  - Generate transaction IDs
 *  - Validate stock before adding items
 *  - Fetch live unit price from Item table
 *  - Compute lineTotal (quantity × unitPrice)
 *  - Guard all mutations against non-ACTIVE transactions
 */
public class TransactionService {

    // ----------------------------------------------------------------
    // DAO dependencies (instantiated once per service instance)
    // ----------------------------------------------------------------
    private final SalesTransactionDAO txnDAO;
    private final TransactionItemDAO  txnItemDAO;
    private final InventoryRecordDAO  inventoryDAO;
    private final ItemDAO             itemDAO;

    public TransactionService() {
        this.txnDAO      = new SalesTransactionDAO();
        this.txnItemDAO  = new TransactionItemDAO();
        this.inventoryDAO = new InventoryRecordDAO();
        this.itemDAO      = new ItemDAO();
    }

    // ================================================================
    // 1. createTransaction(String salesStaffId)
    // ================================================================

    /**
     * Creates a new ACTIVE sales transaction for the specified staff member.
     *
     * @param salesStaffId the userId of the SalesStaff creating the transaction
     * @return the generated transactionId, or {@code null} if creation failed
     */
    public String createTransaction(String salesStaffId) {
        if (salesStaffId == null || salesStaffId.isBlank()) {
            System.err.println("createTransaction() failed – salesStaffId must not be empty.");
            return null;
        }

        // Generate a readable, time-based unique ID.
        String transactionId = "TXN" + System.currentTimeMillis();

        SalesTransaction txn = new SalesTransaction();
        txn.setTransactionId(transactionId);
        txn.setTransactionDate(LocalDateTime.now());
        txn.setTotalAmount(0.00);
        txn.setStatus("ACTIVE");
        txn.setSalesStaffId(salesStaffId);

        boolean created = txnDAO.createTransaction(txn);
        if (created) {
            System.out.println("Transaction created: " + transactionId);
            return transactionId;
        }

        System.err.println("createTransaction() failed for staffId: " + salesStaffId);
        return null;
    }

    // ================================================================
    // 2. addItemToTransaction(String transactionId, String itemCode, int quantity)
    // ================================================================

    /**
     * Adds a product to an ACTIVE transaction after validating stock availability.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Verify the transaction exists and is ACTIVE.</li>
     *   <li>Fetch the item's current selling price from the Item table.</li>
     *   <li>Check that sufficient stock is available in InventoryRecord.</li>
     *   <li>Insert the TransactionItem; lineTotal is computed automatically.</li>
     * </ol>
     * Inventory is NOT decremented here – the DB trigger handles that atomically
     * when the transaction is FINALIZED.</p>
     *
     * @param transactionId the target transaction (must be ACTIVE)
     * @param itemCode      the item to add
     * @param quantity      units to add (must be &gt; 0)
     * @return {@code true} if the line item was added successfully
     */
    public boolean addItemToTransaction(String transactionId, String itemCode, int quantity) {
        if (quantity <= 0) {
            System.err.println("addItemToTransaction() failed – quantity must be > 0.");
            return false;
        }

        // --- Guard: transaction must be ACTIVE ---
        SalesTransaction txn = txnDAO.getTransactionById(transactionId);
        if (txn == null) {
            System.err.println("addItemToTransaction() failed – transaction not found: " + transactionId);
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("addItemToTransaction() failed – transaction '" + transactionId
                               + "' is " + txn.getStatus() + ", not ACTIVE.");
            return false;
        }

        // --- Fetch live unit price from Item master ---
        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("addItemToTransaction() failed – item not found: " + itemCode);
            return false;
        }

        // --- Validate stock availability ---
        int currentStock = inventoryDAO.getStockLevel(itemCode);
        if (currentStock < 0) {
            System.err.println("addItemToTransaction() failed – no inventory record for item: " + itemCode);
            return false;
        }
        if (currentStock < quantity) {
            System.err.println("addItemToTransaction() failed – insufficient stock for '" + itemCode
                               + "'. Requested: " + quantity + ", Available: " + currentStock);
            return false;
        }

        // --- Build and insert the TransactionItem ---
        TransactionItem txnItem = new TransactionItem();
        txnItem.setTransactionId(transactionId);
        txnItem.setItemCode(itemCode);
        txnItem.setQuantity(quantity);
        txnItem.setUnitPrice(item.getPrice());           // live price snapshot
        txnItem.setLineTotal(quantity * item.getPrice()); // DAO will recompute, but set here too

        return txnItemDAO.addTransactionItem(txnItem);
    }

    // ================================================================
    // 3. updateItemQuantity(int transactionItemId, int newQuantity)
    // ================================================================

    /**
     * Updates the quantity of an existing line item in an ACTIVE transaction.
     *
     * <p>Stock is re-validated against the new quantity:
     * existing reserved quantity is returned first, then the new amount is
     * checked against current stock to avoid over-selling.</p>
     *
     * @param transactionItemId PK of the TransactionItem row
     * @param newQuantity       replacement quantity (must be &gt; 0)
     * @return {@code true} if the update succeeded
     */
    public boolean updateItemQuantity(int transactionItemId, int newQuantity) {
        if (newQuantity <= 0) {
            System.err.println("updateItemQuantity() failed – quantity must be > 0. " +
                               "Use removeItemFromTransaction() to delete.");
            return false;
        }

        // --- Fetch existing line item ---
        TransactionItem existing = txnItemDAO.getTransactionItemById(transactionItemId);
        if (existing == null) {
            System.err.println("updateItemQuantity() failed – TransactionItem not found: " + transactionItemId);
            return false;
        }

        // --- Guard: parent transaction must still be ACTIVE ---
        SalesTransaction txn = txnDAO.getTransactionById(existing.getTransactionId());
        if (txn == null || !"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("updateItemQuantity() failed – transaction '"
                               + existing.getTransactionId() + "' is not ACTIVE.");
            return false;
        }

        // --- Stock check: available stock + quantity already in cart >= newQuantity ---
        int currentStock = inventoryDAO.getStockLevel(existing.getItemCode());
        // Stock reserved for this line item is not yet decremented (trigger fires on FINALIZE).
        // So effective available = currentStock (full stock minus nothing yet).
        // We compare newQuantity against currentStock directly.
        if (currentStock < newQuantity) {
            System.err.println("updateItemQuantity() failed – insufficient stock for '"
                               + existing.getItemCode() + "'. Requested: " + newQuantity
                               + ", Available: " + currentStock);
            return false;
        }

        return txnItemDAO.updateTransactionItemQuantity(transactionItemId, newQuantity);
    }

    // ================================================================
    // 4. removeItemFromTransaction(int transactionItemId)
    // ================================================================

    /**
     * Removes a line item from a transaction.
     *
     * <p>The parent transaction must be ACTIVE. The DB trigger
     * {@code trg_RecalcTotalOnDelete} updates the running total automatically.</p>
     *
     * @param transactionItemId PK of the TransactionItem row to delete
     * @return {@code true} if the row was removed
     */
    public boolean removeItemFromTransaction(int transactionItemId) {
        // --- Fetch existing line item to get parent transactionId ---
        TransactionItem existing = txnItemDAO.getTransactionItemById(transactionItemId);
        if (existing == null) {
            System.err.println("removeItemFromTransaction() failed – item not found: " + transactionItemId);
            return false;
        }

        // --- Guard: parent transaction must be ACTIVE ---
        SalesTransaction txn = txnDAO.getTransactionById(existing.getTransactionId());
        if (txn == null || !"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("removeItemFromTransaction() failed – transaction '"
                               + existing.getTransactionId() + "' is not ACTIVE.");
            return false;
        }

        return txnItemDAO.removeTransactionItem(transactionItemId);
    }

    // ================================================================
    // 5. finalizeTransaction(String transactionId)
    // ================================================================

    /**
     * Finalizes a transaction, locking it from further edits.
     *
     * <p>The DB trigger {@code trg_UpdateInventoryOnFinalize} atomically
     * decrements stock for every line item once the status flips.</p>
     *
     * <p>Pre-conditions checked:
     * <ul>
     *   <li>Transaction must exist and be ACTIVE.</li>
     *   <li>Transaction must contain at least one line item.</li>
     * </ul>
     * </p>
     *
     * @param transactionId the transaction to finalize
     * @return {@code true} if finalized successfully
     */
    public boolean finalizeTransaction(String transactionId) {
        SalesTransaction txn = txnDAO.getTransactionById(transactionId);
        if (txn == null) {
            System.err.println("finalizeTransaction() failed – transaction not found: " + transactionId);
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("finalizeTransaction() failed – transaction is "
                               + txn.getStatus() + ", not ACTIVE.");
            return false;
        }

        // Reject empty transactions (no items to bill).
        if (txnItemDAO.getItemsByTransactionId(transactionId).isEmpty()) {
            System.err.println("finalizeTransaction() failed – transaction '" + transactionId
                               + "' has no items. Add items before finalizing.");
            return false;
        }

        return txnDAO.finalizeTransaction(transactionId);
    }

    // ================================================================
    // 6. abortTransaction(String transactionId)
    // ================================================================

    /**
     * Aborts an ACTIVE transaction without affecting inventory.
     *
     * <p>Since stock is only decremented on FINALIZE, aborting an ACTIVE
     * transaction leaves inventory untouched.</p>
     *
     * @param transactionId the transaction to abort
     * @return {@code true} if aborted successfully
     */
    public boolean abortTransaction(String transactionId) {
        SalesTransaction txn = txnDAO.getTransactionById(transactionId);
        if (txn == null) {
            System.err.println("abortTransaction() failed – transaction not found: " + transactionId);
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("abortTransaction() failed – transaction is "
                               + txn.getStatus() + ", not ACTIVE.");
            return false;
        }

        return txnDAO.abortTransaction(transactionId);
    }
}

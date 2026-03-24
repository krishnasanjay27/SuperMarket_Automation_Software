package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Item;
import model.SalesTransaction;
import model.TransactionItem;

import java.time.LocalDateTime;

public class TransactionService {

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

    public String createTransaction(String salesStaffId) {
        if (salesStaffId == null || salesStaffId.isBlank()) {
            System.err.println("createTransaction() failed – salesStaffId must not be empty.");
            return null;
        }

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

    public boolean addItemToTransaction(String transactionId, String itemCode, int quantity) {
        if (quantity <= 0) {
            System.err.println("addItemToTransaction() failed – quantity must be > 0.");
            return false;
        }

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

        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("addItemToTransaction() failed – item not found: " + itemCode);
            return false;
        }

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

        TransactionItem txnItem = new TransactionItem();
        txnItem.setTransactionId(transactionId);
        txnItem.setItemCode(itemCode);
        txnItem.setQuantity(quantity);
        txnItem.setUnitPrice(item.getPrice());
        txnItem.setLineTotal(quantity * item.getPrice());

        return txnItemDAO.addTransactionItem(txnItem);
    }

    public boolean updateItemQuantity(int transactionItemId, int newQuantity) {
        if (newQuantity <= 0) {
            System.err.println("updateItemQuantity() failed – quantity must be > 0. " +
                               "Use removeItemFromTransaction() to delete.");
            return false;
        }

        TransactionItem existing = txnItemDAO.getTransactionItemById(transactionItemId);
        if (existing == null) {
            System.err.println("updateItemQuantity() failed – TransactionItem not found: " + transactionItemId);
            return false;
        }

        SalesTransaction txn = txnDAO.getTransactionById(existing.getTransactionId());
        if (txn == null || !"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("updateItemQuantity() failed – transaction '"
                               + existing.getTransactionId() + "' is not ACTIVE.");
            return false;
        }

        int currentStock = inventoryDAO.getStockLevel(existing.getItemCode());
        if (currentStock < newQuantity) {
            System.err.println("updateItemQuantity() failed – insufficient stock for '"
                               + existing.getItemCode() + "'. Requested: " + newQuantity
                               + ", Available: " + currentStock);
            return false;
        }

        return txnItemDAO.updateTransactionItemQuantity(transactionItemId, newQuantity);
    }

    public boolean removeItemFromTransaction(int transactionItemId) {
        TransactionItem existing = txnItemDAO.getTransactionItemById(transactionItemId);
        if (existing == null) {
            System.err.println("removeItemFromTransaction() failed – item not found: " + transactionItemId);
            return false;
        }

        SalesTransaction txn = txnDAO.getTransactionById(existing.getTransactionId());
        if (txn == null || !"ACTIVE".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("removeItemFromTransaction() failed – transaction '"
                               + existing.getTransactionId() + "' is not ACTIVE.");
            return false;
        }

        return txnItemDAO.removeTransactionItem(transactionItemId);
    }

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

        if (txnItemDAO.getItemsByTransactionId(transactionId).isEmpty()) {
            System.err.println("finalizeTransaction() failed – transaction '" + transactionId
                               + "' has no items. Add items before finalizing.");
            return false;
        }

        return txnDAO.finalizeTransaction(transactionId);
    }

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

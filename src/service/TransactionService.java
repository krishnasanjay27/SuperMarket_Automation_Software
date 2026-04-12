package service;

import dao.BillDAO;
import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Bill;
import model.Item;
import model.SalesTransaction;
import model.TransactionItem;

import java.time.LocalDateTime;
import java.util.List;

public class TransactionService {

    private final SalesTransactionDAO txnDAO;
    private final TransactionItemDAO  txnItemDAO;
    private final InventoryRecordDAO  inventoryDAO;
    private final ItemDAO             itemDAO;
    private final BillDAO             billDAO;
    private final CustomerService     customerService;

    public TransactionService() {
        this.txnDAO          = new SalesTransactionDAO();
        this.txnItemDAO      = new TransactionItemDAO();
        this.inventoryDAO    = new InventoryRecordDAO();
        this.itemDAO         = new ItemDAO();
        this.billDAO         = new BillDAO();
        this.customerService = new CustomerService();
    }

    /** Constructor for dependency injection (testing). */
    public TransactionService(SalesTransactionDAO txnDAO, TransactionItemDAO txnItemDAO,
                              InventoryRecordDAO inventoryDAO, ItemDAO itemDAO,
                              BillDAO billDAO, CustomerService customerService) {
        this.txnDAO          = txnDAO;
        this.txnItemDAO      = txnItemDAO;
        this.inventoryDAO    = inventoryDAO;
        this.itemDAO         = itemDAO;
        this.billDAO         = billDAO;
        this.customerService = customerService;
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
        txn.setCustomerId(null);

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

    /**
     * Finalizes a transaction with optional customer loyalty points.
     * 
     * Workflow (Change 6 ordering):
     * 1. Validate transaction status
     * 2. Validate pointsToRedeem
     * 3. Calculate subtotal
     * 4. Calculate discount (capped at subtotal so finalTotal >= 0)
     * 5. Assign customerId to transaction
     * 6. Finalize transaction (DB trigger deducts inventory)
     * 7. Redeem customer points
     * 8. Calculate and add earned points
     * 9. Build Bill with loyaltyPointsEarned
     * 10. Persist Bill
     *
     * @param transactionId  active transaction ID
     * @param customerId     0 or negative = no customer
     * @param pointsToRedeem loyalty points the customer wants to use
     * @return true on success
     */
    public boolean finalizeTransaction(String transactionId, int customerId, int pointsToRedeem) {

        // Step 1 – Validate transaction status
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

        List<TransactionItem> items = txnItemDAO.getItemsByTransactionId(transactionId);
        if (items.isEmpty()) {
            System.err.println("finalizeTransaction() failed – no items in transaction '" + transactionId + "'.");
            return false;
        }

        // Step 2 – Validate pointsToRedeem
        boolean hasCustomer = customerId > 0;
        if (pointsToRedeem < 0) {
            System.err.println("finalizeTransaction() failed – pointsToRedeem cannot be negative.");
            return false;
        }
        if (hasCustomer && pointsToRedeem > 0
                && !customerService.hasEnoughPoints(customerId, pointsToRedeem)) {
            System.err.println("finalizeTransaction() failed – insufficient loyalty points.");
            return false;
        }

        // Step 3 – Calculate subtotal
        double subtotal = items.stream().mapToDouble(TransactionItem::getLineTotal).sum();

        // Step 4 – Calculate discount; cap at subtotal to prevent negative final total
        double discount = hasCustomer ? customerService.calculateDiscount(pointsToRedeem) : 0.0;
        if (discount > subtotal) {
            discount = subtotal;
        }
        double finalTotal = subtotal - discount;

        // Step 5 – Link customer to transaction
        if (hasCustomer) {
            txnDAO.setCustomerId(transactionId, customerId);
        }

        // Step 6 – Finalize transaction (triggers inventory deduction in DB)
        boolean finalized = txnDAO.finalizeTransaction(transactionId);
        if (!finalized) {
            System.err.println("finalizeTransaction() failed – DB finalize step failed.");
            return false;
        }

        // Step 7 – Redeem points
        if (hasCustomer && pointsToRedeem > 0) {
            customerService.redeemPoints(customerId, pointsToRedeem);
        }

        // Step 8 – Calculate and award earned points (1 point per ₹100)
        int earnedPoints = customerService.calculateEarnedPoints(subtotal);
        if (hasCustomer && earnedPoints > 0) {
            customerService.addLoyaltyPoints(customerId, earnedPoints);
        }

        // Step 9 & 10 – Build and persist Bill with all loyalty data
        Bill bill = new Bill();
        bill.setTransactionId(transactionId);
        bill.setGeneratedDate(LocalDateTime.now());
        bill.setTotalAmount(subtotal);
        bill.setLoyaltyPointsUsed(pointsToRedeem);
        bill.setLoyaltyDiscount(discount);
        bill.setFinalTotal(finalTotal);
        bill.setLoyaltyPointsEarned(earnedPoints);

        boolean billSaved = billDAO.generateBill(bill);
        if (!billSaved) {
            System.err.println("finalizeTransaction() – Bill could not be saved for: " + transactionId);
        }

        System.out.printf("Finalized: %s | Subtotal=%.2f | Discount=%.2f | Final=%.2f | " +
                          "PointsUsed=%d | PointsEarned=%d%n",
                transactionId, subtotal, discount, finalTotal, pointsToRedeem, earnedPoints);

        return true;
    }

    /**
     * Backward-compatible overload – finalizes without any customer or loyalty points.
     */
    public boolean finalizeTransaction(String transactionId) {
        return finalizeTransaction(transactionId, 0, 0);
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

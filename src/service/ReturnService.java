package service;

import config.DBConnection;
import dao.ReturnDAO;
import dao.SalesTransactionDAO;
import model.ReturnTransaction;
import model.ReturnableItemDTO;
import model.SalesTransaction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class ReturnService {

    private final ReturnDAO            returnDAO;
    private final SalesTransactionDAO  salesTransactionDAO;
    private final CustomerService      customerService;

    public ReturnService() {
        this.returnDAO           = new ReturnDAO();
        this.salesTransactionDAO = new SalesTransactionDAO();
        this.customerService     = new CustomerService();
    }

    /** Constructor for dependency injection (testing). */
    public ReturnService(ReturnDAO returnDAO, SalesTransactionDAO salesTransactionDAO,
                         CustomerService customerService) {
        this.returnDAO           = returnDAO;
        this.salesTransactionDAO = salesTransactionDAO;
        this.customerService     = customerService;
    }

    /**
     * Validates that the transaction exists AND is FINALIZED.
     * Adjustment 4: returns are blocked for any other status.
     *
     * @return the SalesTransaction, or null if invalid.
     */
    public SalesTransaction validateTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            System.err.println("validateTransaction() – transactionId must not be empty.");
            return null;
        }
        SalesTransaction txn = salesTransactionDAO.getTransactionById(transactionId);
        if (txn == null) {
            System.err.println("validateTransaction() – transaction not found: " + transactionId);
            return null;
        }
        if (!"FINALIZED".equals(txn.getStatus())) {
            System.err.println("validateTransaction() – transaction " + transactionId
                               + " is not FINALIZED (status=" + txn.getStatus() + ").");
            return null;
        }
        return txn;
    }

    /**
     * Returns the list of items in a transaction with eligibility status
     * computed inside the DAO query.
     */
    public List<ReturnableItemDTO> getReturnableItems(String transactionId) {
        return returnDAO.getTransactionItems(transactionId);
    }

    /**
     * Checks return eligibility for a specific item in a transaction.
     * Convenience method for programmatic checks (validation flow).
     */
    public boolean checkReturnEligibility(String transactionId, String itemCode) {
        List<ReturnableItemDTO> items = returnDAO.getTransactionItems(transactionId);
        return items.stream()
                    .filter(dto -> dto.getItemCode().equals(itemCode))
                    .anyMatch(dto -> "Returnable".equals(dto.getEligibilityStatus()));
    }

    /**
     * Returns how many units of an item can still be returned.
     */
    public int getRemainingReturnableQuantity(String transactionId, String itemCode) {
        return returnDAO.getRemainingReturnableQuantity(transactionId, itemCode);
    }

    /**
     * Calculates the refund amount using the original transaction unit price.
     * Adjustment 5 (Module 5): always uses TransactionItem.unitPrice, never Item.price.
     */
    public double calculateRefundAmount(String transactionId, String itemCode, int quantity) {
        double unitPrice = returnDAO.getOriginalUnitPrice(transactionId, itemCode);
        return unitPrice * quantity;
    }

    /**
     * Processes a return atomically:
     *   1. Validates the transaction is FINALIZED
     *   2. Checks item eligibility (duration window + remaining qty)
     *   3. Inserts ReturnTransaction record
     *   4. Updates InventoryRecord (stockLevel += returnedQty)
     *
     * Both the insert and the inventory update share the same Connection and are
     * committed or rolled back together.
     *
     * @return true on success, false on any validation or DB failure.
     */
    public boolean processReturn(String transactionId, String itemCode,
                                 int quantity, String processedBy, String reason) {

        // --- Validation ---
        if (validateTransaction(transactionId) == null) {
            return false;
        }

        if (!checkReturnEligibility(transactionId, itemCode)) {
            System.err.println("processReturn() – item '" + itemCode
                               + "' is not eligible for return in transaction " + transactionId);
            return false;
        }

        int remaining = getRemainingReturnableQuantity(transactionId, itemCode);
        if (quantity <= 0 || quantity > remaining) {
            System.err.println("processReturn() – invalid quantity " + quantity
                               + " (remaining returnable: " + remaining + ")");
            return false;
        }

        double refundAmount = calculateRefundAmount(transactionId, itemCode, quantity);

        ReturnTransaction rt = new ReturnTransaction(
                transactionId, itemCode, quantity, refundAmount, processedBy, reason);
        rt.setReturnDate(LocalDateTime.now());

        // --- Atomic DB operation ---
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted    = returnDAO.insertReturnRecord(conn, rt);
                boolean inventoryOk = true;
                
                if (!"Damaged".equalsIgnoreCase(reason)) {
                    inventoryOk = returnDAO.updateInventoryAfterReturn(conn, itemCode, quantity);
                }

                if (inserted && inventoryOk) {
                    conn.commit();
                    System.out.println("Return processed: " + quantity + "x " + itemCode
                                       + " → refund ₹" + String.format("%.2f", refundAmount));
                    
                    // Handle loyalty points: deduct original logic & credit refund value
                    SalesTransaction txn = salesTransactionDAO.getTransactionById(transactionId);
                    if (txn != null && txn.getCustomerId() != null && txn.getCustomerId() > 0) {
                        int pointsToDeduct = customerService.calculateEarnedPoints(refundAmount);
                        if (pointsToDeduct > 0) {
                            customerService.deductLoyaltyPoints(txn.getCustomerId(), pointsToDeduct);
                            System.out.println("Deducted " + pointsToDeduct + " points from customer " + txn.getCustomerId());
                        }

                        // Store credit refund: 1 point = 1 rupee
                        int pointsToRefund = (int) refundAmount;
                        if (pointsToRefund > 0) {
                            customerService.addLoyaltyPoints(txn.getCustomerId(), pointsToRefund);
                            System.out.println("Added " + pointsToRefund + " points as store credit to customer " + txn.getCustomerId());
                        }
                    }

                    return true;
                } else {
                    conn.rollback();
                    System.err.println("processReturn() – operation incomplete, rolled back.");
                    return false;
                }
            } catch (Exception e) {
                conn.rollback();
                System.err.println("processReturn() – exception, rolled back: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("processReturn() – DB connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches the full return history for a given transaction.
     */
    public List<ReturnTransaction> getReturnHistory(String transactionId) {
        return returnDAO.getReturnHistory(transactionId);
    }

    /**
     * Updates inventory stock level after return (standalone, if needed).
     * The primary flow uses the transactional version in processReturn().
     */
    public boolean updateInventoryAfterReturn(String itemCode, int quantity) {
        try (Connection conn = DBConnection.getConnection()) {
            return returnDAO.updateInventoryAfterReturn(conn, itemCode, quantity);
        } catch (Exception e) {
            System.err.println("updateInventoryAfterReturn() failed – " + e.getMessage());
            return false;
        }
    }
}

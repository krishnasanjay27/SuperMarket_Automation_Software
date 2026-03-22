package dao;

import config.DBConnection;
import model.TransactionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionItemDAO – JDBC Data Access Object for the TransactionItem table.
 *
 * lineTotal is always computed as quantity × unitPrice in Java before any
 * INSERT or UPDATE, keeping the stored value consistent with the triggers
 * that maintain SalesTransaction.totalAmount.
 */
public class TransactionItemDAO {

    // ----------------------------------------------------------------
    // SQL constants
    // ----------------------------------------------------------------
    private static final String SQL_ADD =
            "INSERT INTO TransactionItem (transactionId, itemCode, quantity, unitPrice, lineTotal) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_GET_BY_TXN =
            "SELECT transactionItemId, transactionId, itemCode, quantity, unitPrice, lineTotal " +
            "FROM TransactionItem WHERE transactionId = ? ORDER BY transactionItemId";

    private static final String SQL_GET_BY_ID =
            "SELECT transactionItemId, transactionId, itemCode, quantity, unitPrice, lineTotal " +
            "FROM TransactionItem WHERE transactionItemId = ?";

    private static final String SQL_GET_UNIT_PRICE =
            "SELECT unitPrice FROM TransactionItem WHERE transactionItemId = ?";

    private static final String SQL_UPDATE_QTY =
            "UPDATE TransactionItem SET quantity = ?, lineTotal = ? WHERE transactionItemId = ?";

    private static final String SQL_REMOVE =
            "DELETE FROM TransactionItem WHERE transactionItemId = ?";

    // ================================================================
    // 1. addTransactionItem(TransactionItem item)
    // ================================================================

    /**
     * Inserts a new line item into a transaction.
     *
     * <p>lineTotal is computed here as {@code quantity × unitPrice} before
     * the INSERT, ensuring the stored value always matches the calculation.</p>
     *
     * @param item a populated {@link TransactionItem}; quantity must be > 0
     * @return {@code true} if the row was inserted successfully
     */
    public boolean addTransactionItem(TransactionItem item) {
        if (item.getQuantity() <= 0) {
            System.err.println("addTransactionItem() failed – quantity must be greater than 0.");
            return false;
        }

        boolean success = false;

        // Recompute lineTotal in Java before storing.
        double lineTotal = item.getQuantity() * item.getUnitPrice();
        item.setLineTotal(lineTotal);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ADD,
                                         Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getTransactionId());
            stmt.setString(2, item.getItemCode());
            stmt.setInt(3, item.getQuantity());
            stmt.setDouble(4, item.getUnitPrice());
            stmt.setDouble(5, item.getLineTotal());

            success = stmt.executeUpdate() > 0;

            if (success) {
                // Capture the AUTO_INCREMENT id and write it back to the object.
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        item.setTransactionItemId(keys.getInt(1));
                    }
                }
                System.out.println("TransactionItem added: itemCode='" + item.getItemCode()
                                   + "', qty=" + item.getQuantity()
                                   + ", lineTotal=" + item.getLineTotal());
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("addTransactionItem() blocked – duplicate (transactionId, itemCode): "
                               + item.getTransactionId() + " / " + item.getItemCode()
                               + ". Use updateTransactionItemQuantity() instead.");
        } catch (SQLException e) {
            System.err.println("addTransactionItem() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 2. getItemsByTransactionId(String transactionId)
    // ================================================================

    /**
     * Returns all line items belonging to a specific transaction,
     * ordered by insertion sequence.
     *
     * @param transactionId the parent transaction ID
     * @return list of {@link TransactionItem} objects; empty if none
     */
    public List<TransactionItem> getItemsByTransactionId(String transactionId) {
        List<TransactionItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_TXN)) {

            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("getItemsByTransactionId() failed – " + e.getMessage());
        }

        return list;
    }

    // ================================================================
    // 3. updateTransactionItemQuantity(int transactionItemId, int newQuantity)
    // ================================================================

    /**
     * Updates the quantity of an existing line item and recalculates lineTotal.
     *
     * <p>lineTotal is recomputed as {@code newQuantity × unitPrice} using the
     * unitPrice already stored in the database, so price accuracy is preserved.</p>
     *
     * @param transactionItemId the primary key of the line item to update
     * @param newQuantity       the replacement quantity (must be > 0)
     * @return {@code true} if the update succeeded
     */
    public boolean updateTransactionItemQuantity(int transactionItemId, int newQuantity) {
        if (newQuantity <= 0) {
            System.err.println("updateTransactionItemQuantity() failed – quantity must be > 0. " +
                               "Use removeTransactionItem() to delete an item.");
            return false;
        }

        boolean success = false;

        // Fetch the stored unitPrice to recompute lineTotal accurately.
        double unitPrice = fetchUnitPrice(transactionItemId);
        if (unitPrice < 0) {
            System.err.println("updateTransactionItemQuantity() failed – item not found (id="
                               + transactionItemId + ").");
            return false;
        }

        double newLineTotal = newQuantity * unitPrice;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_QTY)) {

            stmt.setInt(1, newQuantity);
            stmt.setDouble(2, newLineTotal);
            stmt.setInt(3, transactionItemId);

            success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("TransactionItem id=" + transactionItemId
                                   + " updated: qty=" + newQuantity
                                   + ", lineTotal=" + newLineTotal);
            }

        } catch (SQLException e) {
            System.err.println("updateTransactionItemQuantity() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 4. removeTransactionItem(int transactionItemId)
    // ================================================================

    /**
     * Deletes a single line item from a transaction.
     *
     * <p>The {@code trg_RecalcTotalOnDelete} trigger automatically adjusts
     * {@code SalesTransaction.totalAmount} after this delete.</p>
     *
     * @param transactionItemId the primary key of the line item to remove
     * @return {@code true} if the row was deleted
     */
    public boolean removeTransactionItem(int transactionItemId) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REMOVE)) {

            stmt.setInt(1, transactionItemId);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("TransactionItem id=" + transactionItemId + " removed.");
            } else {
                System.out.println("removeTransactionItem() – no item found with id=" + transactionItemId);
            }

        } catch (SQLException e) {
            System.err.println("removeTransactionItem() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 5. getTransactionItemById(int transactionItemId)
    // ================================================================

    /**
     * Retrieves a single line item by its auto-increment primary key.
     *
     * @param transactionItemId the primary key to look up
     * @return the matching {@link TransactionItem}, or {@code null} if not found
     */
    public TransactionItem getTransactionItemById(int transactionItemId) {
        TransactionItem item = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setInt(1, transactionItemId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    item = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getTransactionItemById() failed – " + e.getMessage());
        }

        return item;
    }

    // ================================================================
    // Private helper – fetch the stored unitPrice for a line item
    // ================================================================
    private double fetchUnitPrice(int transactionItemId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_UNIT_PRICE)) {

            stmt.setInt(1, transactionItemId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("unitPrice");
                }
            }

        } catch (SQLException e) {
            System.err.println("fetchUnitPrice() failed – " + e.getMessage());
        }

        return -1; // signals "not found"
    }

    // ================================================================
    // Private helper – maps a ResultSet row to a TransactionItem object
    // ================================================================
    private TransactionItem mapRow(ResultSet rs) throws SQLException {
        TransactionItem item = new TransactionItem();
        item.setTransactionItemId(rs.getInt("transactionItemId"));
        item.setTransactionId(rs.getString("transactionId"));
        item.setItemCode(rs.getString("itemCode"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unitPrice"));
        item.setLineTotal(rs.getDouble("lineTotal"));
        return item;
    }
}

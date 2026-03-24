package dao;

import config.DBConnection;
import model.TransactionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionItemDAO {

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

    public boolean addTransactionItem(TransactionItem item) {
        if (item.getQuantity() <= 0) {
            System.err.println("addTransactionItem() failed – quantity must be greater than 0.");
            return false;
        }

        boolean success = false;

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

    public boolean updateTransactionItemQuantity(int transactionItemId, int newQuantity) {
        if (newQuantity <= 0) {
            System.err.println("updateTransactionItemQuantity() failed – quantity must be > 0. " +
                               "Use removeTransactionItem() to delete an item.");
            return false;
        }

        boolean success = false;

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

        return -1;
    }

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

package dao;

import config.DBConnection;
import model.SalesTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesTransactionDAO {

    private static final String SQL_CREATE =
            "INSERT INTO SalesTransaction (transactionId, transactionDate, totalAmount, status, salesStaffId) " +
            "VALUES (?, ?, ?, 'ACTIVE', ?)";

    private static final String SQL_GET_BY_ID =
            "SELECT transactionId, transactionDate, totalAmount, status, salesStaffId " +
            "FROM SalesTransaction WHERE transactionId = ?";

    private static final String SQL_GET_ACTIVE =
            "SELECT transactionId, transactionDate, totalAmount, status, salesStaffId " +
            "FROM SalesTransaction WHERE status = 'ACTIVE' ORDER BY transactionDate DESC";

    private static final String SQL_FINALIZE =
            "UPDATE SalesTransaction SET status = 'FINALIZED' " +
            "WHERE transactionId = ? AND status = 'ACTIVE'";

    private static final String SQL_ABORT =
            "UPDATE SalesTransaction SET status = 'ABORTED' " +
            "WHERE transactionId = ? AND status = 'ACTIVE'";

    private static final String SQL_UPDATE_TOTAL =
            "UPDATE SalesTransaction SET totalAmount = ? WHERE transactionId = ?";

    private static final String SQL_GET_BY_STAFF =
            "SELECT transactionId, transactionDate, totalAmount, status, salesStaffId " +
            "FROM SalesTransaction WHERE salesStaffId = ? ORDER BY transactionDate DESC";

    public boolean createTransaction(SalesTransaction transaction) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CREATE)) {

            stmt.setString(1, transaction.getTransactionId());

            LocalDateTime date = (transaction.getTransactionDate() != null)
                                 ? transaction.getTransactionDate() : LocalDateTime.now();
            stmt.setTimestamp(2, Timestamp.valueOf(date));

            stmt.setDouble(3, transaction.getTotalAmount());
            stmt.setString(4, transaction.getSalesStaffId());

            success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("Transaction '" + transaction.getTransactionId() + "' created.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("createTransaction() failed – duplicate transactionId: "
                               + transaction.getTransactionId());
        } catch (SQLException e) {
            System.err.println("createTransaction() failed – " + e.getMessage());
        }

        return success;
    }

    public SalesTransaction getTransactionById(String transactionId) {
        SalesTransaction txn = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    txn = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getTransactionById() failed – " + e.getMessage());
        }

        return txn;
    }

    public List<SalesTransaction> getActiveTransactions() {
        List<SalesTransaction> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ACTIVE);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("getActiveTransactions() failed – " + e.getMessage());
        }

        return list;
    }

    public boolean finalizeTransaction(String transactionId) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FINALIZE)) {

            stmt.setString(1, transactionId);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("Transaction '" + transactionId + "' finalized. Inventory updated by trigger.");
            } else {
                System.out.println("finalizeTransaction() – transaction not found or not in ACTIVE state.");
            }

        } catch (SQLException e) {
            System.err.println("finalizeTransaction() failed – " + e.getMessage());
        }

        return success;
    }

    public boolean abortTransaction(String transactionId) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ABORT)) {

            stmt.setString(1, transactionId);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("Transaction '" + transactionId + "' aborted.");
            } else {
                System.out.println("abortTransaction() – transaction not found or not in ACTIVE state.");
            }

        } catch (SQLException e) {
            System.err.println("abortTransaction() failed – " + e.getMessage());
        }

        return success;
    }

    public boolean updateTotalAmount(String transactionId, double totalAmount) {
        if (totalAmount < 0) {
            System.err.println("updateTotalAmount() failed – totalAmount cannot be negative.");
            return false;
        }

        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_TOTAL)) {

            stmt.setDouble(1, totalAmount);
            stmt.setString(2, transactionId);

            success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("Total updated to " + totalAmount + " for transaction '" + transactionId + "'.");
            } else {
                System.out.println("updateTotalAmount() – no transaction found with ID: " + transactionId);
            }

        } catch (SQLException e) {
            System.err.println("updateTotalAmount() failed – " + e.getMessage());
        }

        return success;
    }

    public List<SalesTransaction> getTransactionsByStaff(String salesStaffId) {
        List<SalesTransaction> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_STAFF)) {

            stmt.setString(1, salesStaffId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("getTransactionsByStaff() failed – " + e.getMessage());
        }

        return list;
    }

    public List<SalesTransaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        List<SalesTransaction> list = new ArrayList<>();

        String sql = "SELECT transactionId, transactionDate, totalAmount, status, salesStaffId " +
                     "FROM SalesTransaction " +
                     "WHERE transactionDate BETWEEN ? AND ? " +
                     "ORDER BY transactionDate DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("getTransactionsByDateRange() failed – " + e.getMessage());
        }

        return list;
    }

    private SalesTransaction mapRow(ResultSet rs) throws SQLException {
        SalesTransaction txn = new SalesTransaction();
        txn.setTransactionId(rs.getString("transactionId"));

        Timestamp ts = rs.getTimestamp("transactionDate");
        if (ts != null) {
            txn.setTransactionDate(ts.toLocalDateTime());
        }

        txn.setTotalAmount(rs.getDouble("totalAmount"));
        txn.setStatus(rs.getString("status"));
        txn.setSalesStaffId(rs.getString("salesStaffId"));
        return txn;
    }
}

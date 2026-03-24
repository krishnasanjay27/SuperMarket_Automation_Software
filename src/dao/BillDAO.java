package dao;

import config.DBConnection;
import model.Bill;

import java.sql.*;
import java.time.LocalDateTime;

public class BillDAO {

    private static final String SQL_INSERT =
            "INSERT INTO Bill (transactionId, generatedDate, totalAmount) " +
            "VALUES (?, ?, ?)";

    private static final String SQL_GET_BY_TXN =
            "SELECT billId, transactionId, generatedDate, totalAmount " +
            "FROM Bill WHERE transactionId = ?";

    private static final String SQL_GET_BY_ID =
            "SELECT billId, transactionId, generatedDate, totalAmount " +
            "FROM Bill WHERE billId = ?";

    public boolean generateBill(Bill bill) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT,
                                         Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, bill.getTransactionId());

            LocalDateTime date = (bill.getGeneratedDate() != null)
                                 ? bill.getGeneratedDate() : LocalDateTime.now();
            stmt.setTimestamp(2, Timestamp.valueOf(date));
            stmt.setDouble(3, bill.getTotalAmount());

            success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        bill.setBillId(keys.getInt(1));
                    }
                }
                System.out.println("Bill generated: billId=" + bill.getBillId()
                                   + " for transaction '" + bill.getTransactionId() + "'.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Bill already exists for transaction: " + bill.getTransactionId());
        } catch (SQLException e) {
            System.err.println("generateBill() failed – " + e.getMessage());
        }

        return success;
    }

    public Bill getBillByTransactionId(String transactionId) {
        Bill bill = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_TXN)) {

            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bill = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getBillByTransactionId() failed – " + e.getMessage());
        }

        return bill;
    }

    public Bill getBillById(int billId) {
        Bill bill = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setInt(1, billId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bill = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getBillById() failed – " + e.getMessage());
        }

        return bill;
    }

    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(rs.getInt("billId"));
        bill.setTransactionId(rs.getString("transactionId"));

        Timestamp ts = rs.getTimestamp("generatedDate");
        if (ts != null) {
            bill.setGeneratedDate(ts.toLocalDateTime());
        }

        bill.setTotalAmount(rs.getDouble("totalAmount"));
        return bill;
    }
}

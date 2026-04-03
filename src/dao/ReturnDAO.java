package dao;

import config.DBConnection;
import model.ReturnTransaction;
import model.ReturnableItemDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnDAO {

    /**
     * Returns all items purchased in a given transaction, with eligibility
     * status computed entirely inside SQL (Adjustment 7).
     *
     * Eligibility = returnDurationDays > 0
     *               AND DATEDIFF(CURRENT_DATE, transactionDate) <= returnDurationDays  (Adj. 1)
     *               AND remainingReturnableQty > 0
     */
    public List<ReturnableItemDTO> getTransactionItems(String transactionId) {
        List<ReturnableItemDTO> list = new ArrayList<>();

        String sql =
            "SELECT " +
            "    ti.itemCode, " +
            "    i.itemName, " +
            "    ti.unitPrice, " +
            "    ti.quantity                                        AS purchasedQty, " +
            "    COALESCE(SUM(rt.quantity), 0)                     AS alreadyReturnedQty, " +
            "    ti.quantity - COALESCE(SUM(rt.quantity), 0)       AS remainingReturnableQty, " +
            "    i.returnDurationDays, " +
            "    st.transactionDate, " +
            "    CASE " +
            "        WHEN i.returnDurationDays = 0 THEN 'Not Returnable' " +
            "        WHEN DATEDIFF(CURRENT_DATE, DATE(st.transactionDate)) > i.returnDurationDays THEN 'Not Returnable' " +
            "        WHEN (ti.quantity - COALESCE(SUM(rt.quantity), 0)) <= 0 THEN 'Not Returnable' " +
            "        ELSE 'Returnable' " +
            "    END AS eligibilityStatus " +
            "FROM TransactionItem ti " +
            "JOIN Item i ON ti.itemCode = i.itemCode " +
            "JOIN SalesTransaction st ON st.transactionId = ti.transactionId " +
            "LEFT JOIN ReturnTransaction rt " +
            "       ON rt.transactionId = ti.transactionId AND rt.itemCode = ti.itemCode " +
            "WHERE ti.transactionId = ? " +
            "  AND st.status = 'FINALIZED' " +
            "GROUP BY ti.itemCode, i.itemName, ti.unitPrice, ti.quantity, " +
            "         i.returnDurationDays, st.transactionDate";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReturnableItemDTO dto = new ReturnableItemDTO();
                    dto.setItemCode(rs.getString("itemCode"));
                    dto.setItemName(rs.getString("itemName"));
                    dto.setUnitPrice(rs.getDouble("unitPrice"));
                    dto.setPurchasedQty(rs.getInt("purchasedQty"));
                    dto.setAlreadyReturnedQty(rs.getInt("alreadyReturnedQty"));
                    dto.setRemainingReturnableQty(rs.getInt("remainingReturnableQty"));
                    dto.setReturnDurationDays(rs.getInt("returnDurationDays"));
                    dto.setEligibilityStatus(rs.getString("eligibilityStatus"));

                    Timestamp ts = rs.getTimestamp("transactionDate");
                    if (ts != null) {
                        dto.setTransactionDate(ts.toLocalDateTime());
                    }

                    list.add(dto);
                }
            }

        } catch (SQLException e) {
            System.err.println("getTransactionItems() failed - " + e.getMessage());
        }

        return list;
    }

    /**
     * Returns the number of units of an item that can still be returned
     * for a given transaction.
     */
    public int getRemainingReturnableQuantity(String transactionId, String itemCode) {
        String sql =
            "SELECT " +
            "    ti.quantity - COALESCE(SUM(rt.quantity), 0) AS remaining " +
            "FROM TransactionItem ti " +
            "LEFT JOIN ReturnTransaction rt " +
            "       ON rt.transactionId = ti.transactionId AND rt.itemCode = ti.itemCode " +
            "WHERE ti.transactionId = ? AND ti.itemCode = ? " +
            "GROUP BY ti.quantity";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);
            stmt.setString(2, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("remaining");
                }
            }

        } catch (SQLException e) {
            System.err.println("getRemainingReturnableQuantity() failed - " + e.getMessage());
        }

        return 0;
    }

    /**
     * Fetches the unit price recorded in the original transaction (used for
     * refund calculation — never uses Item.price).
     */
    public double getOriginalUnitPrice(String transactionId, String itemCode) {
        String sql = "SELECT unitPrice FROM TransactionItem WHERE transactionId = ? AND itemCode = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);
            stmt.setString(2, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("unitPrice");
                }
            }

        } catch (SQLException e) {
            System.err.println("getOriginalUnitPrice() failed - " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Inserts a return record into ReturnTransaction.
     * Does NOT manage its own connection — caller provides a transactional connection.
     */
    public boolean insertReturnRecord(Connection conn, ReturnTransaction rt) throws SQLException {
        String sql =
            "INSERT INTO ReturnTransaction (transactionId, itemCode, quantity, refundAmount, returnDate, processedBy, reason) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rt.getTransactionId());
            stmt.setString(2, rt.getItemCode());
            stmt.setInt(3, rt.getQuantity());
            stmt.setDouble(4, rt.getRefundAmount());

            LocalDateTime returnDate = (rt.getReturnDate() != null) ? rt.getReturnDate() : LocalDateTime.now();
            stmt.setTimestamp(5, Timestamp.valueOf(returnDate));

            stmt.setString(6, rt.getProcessedBy());
            stmt.setString(7, rt.getReason());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Increases stock level for an item after a successful return.
     * Does NOT manage its own connection — caller provides a transactional connection.
     */
    public boolean updateInventoryAfterReturn(Connection conn, String itemCode, int quantity) throws SQLException {
        String sql = "UPDATE InventoryRecord SET stockLevel = stockLevel + ? WHERE itemCode = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, itemCode);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Returns the full return history for a given transaction.
     */
    public List<ReturnTransaction> getReturnHistory(String transactionId) {
        List<ReturnTransaction> list = new ArrayList<>();

        String sql =
            "SELECT returnId, transactionId, itemCode, quantity, refundAmount, returnDate, processedBy, reason " +
            "FROM ReturnTransaction " +
            "WHERE transactionId = ? " +
            "ORDER BY returnDate DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReturnTransaction rt = new ReturnTransaction();
                    rt.setReturnId(rs.getInt("returnId"));
                    rt.setTransactionId(rs.getString("transactionId"));
                    rt.setItemCode(rs.getString("itemCode"));
                    rt.setQuantity(rs.getInt("quantity"));
                    rt.setRefundAmount(rs.getDouble("refundAmount"));
                    rt.setProcessedBy(rs.getString("processedBy"));
                    rt.setReason(rs.getString("reason"));

                    Timestamp ts = rs.getTimestamp("returnDate");
                    if (ts != null) {
                        rt.setReturnDate(ts.toLocalDateTime());
                    }

                    list.add(rt);
                }
            }

        } catch (SQLException e) {
            System.err.println("getReturnHistory() failed - " + e.getMessage());
        }

        return list;
    }
}

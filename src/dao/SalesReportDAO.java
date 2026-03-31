package dao;

import config.DBConnection;
import model.ProfitByItemDTO;
import model.ProfitBySaleDTO;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesReportDAO {

    private static final String SQL_PROFIT_BY_SALE =
        "SELECT st.transactionId, " +
        "       SUM(ti.lineTotal) AS revenue, " +
        "       SUM(ti.quantity * i.costPrice) AS cost, " +
        "       SUM(ti.lineTotal - (ti.quantity * i.costPrice)) AS profit " +
        "FROM SalesTransaction st " +
        "JOIN TransactionItem ti ON st.transactionId = ti.transactionId " +
        "JOIN Item i              ON ti.itemCode = i.itemCode " +
        "WHERE st.status = 'FINALIZED' " +
        "  AND st.transactionDate BETWEEN ? AND ? " +
        "GROUP BY st.transactionId " +
        "ORDER BY st.transactionId";

    private static final String SQL_PROFIT_BY_ITEM =
        "SELECT i.itemName, " +
        "       SUM(ti.quantity) AS totalSold, " +
        "       SUM(ti.quantity * (ti.unitPrice - i.costPrice)) AS profit " +
        "FROM TransactionItem ti " +
        "JOIN Item i              ON ti.itemCode = i.itemCode " +
        "JOIN SalesTransaction st ON st.transactionId = ti.transactionId " +
        "WHERE st.status = 'FINALIZED' " +
        "  AND st.transactionDate BETWEEN ? AND ? " +
        "GROUP BY i.itemName " +
        "ORDER BY profit DESC";

    public List<ProfitBySaleDTO> getProfitBySaleReport(LocalDate startDate, LocalDate endDate) {
        List<ProfitBySaleDTO> results = new ArrayList<>();
        Timestamp start = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp end   = Timestamp.valueOf(endDate.atTime(23, 59, 59));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PROFIT_BY_SALE)) {

            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ProfitBySaleDTO(
                        rs.getString("transactionId"),
                        rs.getDouble("revenue"),
                        rs.getDouble("cost"),
                        rs.getDouble("profit")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("getProfitBySaleReport() failed – " + e.getMessage());
        }
        return results;
    }

    public List<ProfitByItemDTO> getProfitByItemReport(LocalDate startDate, LocalDate endDate) {
        List<ProfitByItemDTO> results = new ArrayList<>();
        Timestamp start = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp end   = Timestamp.valueOf(endDate.atTime(23, 59, 59));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PROFIT_BY_ITEM)) {

            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ProfitByItemDTO(
                        rs.getString("itemName"),
                        rs.getInt("totalSold"),
                        rs.getDouble("profit")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("getProfitByItemReport() failed – " + e.getMessage());
        }
        return results;
    }
}

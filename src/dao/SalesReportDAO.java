package dao;

import config.DBConnection;
import model.ProfitByItemDTO;
import model.ProfitBySaleDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesReportDAO {

    public List<ProfitBySaleDTO> getProfitBySaleReport(LocalDate start, LocalDate end, int limit) {
        List<ProfitBySaleDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    st.transactionId, " +
            "    COALESCE(SUM(ti.lineTotal), 0) AS revenue, " +
            "    COALESCE(SUM(ti.quantity * i.costPrice), 0) AS cost, " +
            "    COALESCE(SUM(ti.lineTotal - (ti.quantity * i.costPrice)), 0) AS profit " +
            "FROM SalesTransaction st " +
            "JOIN TransactionItem ti ON st.transactionId = ti.transactionId " +
            "JOIN Item i ON ti.itemCode = i.itemCode " +
            "WHERE st.status = 'FINALIZED' " +
            "AND st.transactionDate BETWEEN ? AND ? " +
            "GROUP BY st.transactionId " +
            "ORDER BY profit DESC"
        );

        if (limit > 0) {
            sql.append(" LIMIT ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(end.atTime(23, 59, 59)));
            if (limit > 0) {
                stmt.setInt(3, limit);
            }

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
            System.err.println("getProfitBySaleReport() failed - " + e.getMessage());
        }

        return results;
    }

    public List<ProfitByItemDTO> getProfitByItemReport(LocalDate start, LocalDate end, int limit) {
        List<ProfitByItemDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    i.itemCode, " +
            "    i.itemName, " +
            "    COALESCE(SUM(ti.quantity), 0) AS totalSold, " +
            "    COALESCE(SUM(ti.quantity * (ti.unitPrice - i.costPrice)), 0) AS profit " +
            "FROM TransactionItem ti " +
            "JOIN Item i ON ti.itemCode = i.itemCode " +
            "JOIN SalesTransaction st ON st.transactionId = ti.transactionId " +
            "WHERE st.status = 'FINALIZED' " +
            "AND st.transactionDate BETWEEN ? AND ? " +
            "GROUP BY i.itemCode, i.itemName " +
            "ORDER BY profit DESC"
        );

        if (limit > 0) {
            sql.append(" LIMIT ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(end.atTime(23, 59, 59)));
            if (limit > 0) {
                stmt.setInt(3, limit);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ProfitByItemDTO(
                        rs.getString("itemCode"),
                        rs.getString("itemName"),
                        rs.getInt("totalSold"),
                        rs.getDouble("profit")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("getProfitByItemReport() failed - " + e.getMessage());
        }

        return results;
    }
}

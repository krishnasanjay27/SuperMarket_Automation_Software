package dao;

import config.DBConnection;
import model.ProfitByItemDTO;
import model.ProfitBySaleDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesReportDAO {

    /**
     * Profit-by-sale report.
     *
     * Revenue  = SUM(lineTotal) - SUM(refundAmount)
     * Cost     = SUM(quantity * costPrice)
     * Profit   = Revenue - Cost
     *          = SUM(lineTotal) - SUM(refundAmount) - SUM(quantity * costPrice)
     *
     * Note (Adjustment 3): subtracting the full refundAmount from revenue is the
     * correct accounting approach here. The refundAmount already represents the
     * money returned to the customer (qty × unitPrice), so profit is naturally
     * reduced by (refund - cost-saved).  The denominator-level query uses:
     *   net_revenue = SUM(lineTotal) - SUM(refundAmount)
     *   net_cost    = SUM(qty * costPrice) - SUM(returnedQty * costPrice)
     *   profit      = net_revenue - net_cost
     */
    public List<ProfitBySaleDTO> getProfitBySaleReport(LocalDate start, LocalDate end, int limit) {
        List<ProfitBySaleDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    st.transactionId, " +
            "    COALESCE(SUM(ti.lineTotal), 0) - COALESCE(ret_agg.refundAmount, 0)                             AS revenue, " +
            "    COALESCE(SUM(ti.quantity * i.costPrice), 0) - COALESCE(ret_agg.returnedCost, 0)                AS cost, " +
            "    (COALESCE(SUM(ti.lineTotal), 0) - COALESCE(ret_agg.refundAmount, 0)) " +
            "    - (COALESCE(SUM(ti.quantity * i.costPrice), 0) - COALESCE(ret_agg.returnedCost, 0))            AS profit " +
            "FROM SalesTransaction st " +
            "JOIN TransactionItem ti ON st.transactionId = ti.transactionId " +
            "JOIN Item i ON ti.itemCode = i.itemCode " +
            "LEFT JOIN ( " +
            "    SELECT transactionId, " +
            "           SUM(refundAmount)              AS refundAmount, " +
            "           SUM(quantity * ( " +
            "               SELECT costPrice FROM Item ii WHERE ii.itemCode = rt2.itemCode " +
            "           ))                             AS returnedCost " +
            "    FROM ReturnTransaction rt2 " +
            "    GROUP BY transactionId " +
            ") ret_agg ON ret_agg.transactionId = st.transactionId " +
            "WHERE st.status = 'FINALIZED' " +
            "  AND st.transactionDate BETWEEN ? AND ? " +
            "GROUP BY st.transactionId, ret_agg.refundAmount, ret_agg.returnedCost " +
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

    /**
     * Profit-by-item report.
     *
     * Adjustment 2: subtract only the net profit lost per returned unit:
     *   lostProfit = refundAmount - returnedQty * costPrice
     * so:
     *   profit = SUM(qty * (unitPrice - costPrice)) - SUM(refundAmount - returnedQty * costPrice)
     */
    public List<ProfitByItemDTO> getProfitByItemReport(LocalDate start, LocalDate end, int limit) {
        List<ProfitByItemDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    i.itemCode, " +
            "    i.itemName, " +
            "    COALESCE(SUM(ti.quantity), 0) - COALESCE(ret_agg.returnedQty, 0)                       AS totalSold, " +
            "    COALESCE(SUM(ti.quantity * (ti.unitPrice - i.costPrice)), 0) " +
            "    - COALESCE(ret_agg.refundAmount - ret_agg.returnedQty * i.costPrice, 0)                 AS profit " +
            "FROM TransactionItem ti " +
            "JOIN Item i ON ti.itemCode = i.itemCode " +
            "JOIN SalesTransaction st ON st.transactionId = ti.transactionId " +
            "LEFT JOIN ( " +
            "    SELECT itemCode, " +
            "           SUM(quantity)     AS returnedQty, " +
            "           SUM(refundAmount) AS refundAmount " +
            "    FROM ReturnTransaction " +
            "    GROUP BY itemCode " +
            ") ret_agg ON ret_agg.itemCode = ti.itemCode " +
            "WHERE st.status = 'FINALIZED' " +
            "  AND st.transactionDate BETWEEN ? AND ? " +
            "GROUP BY i.itemCode, i.itemName, ret_agg.returnedQty, ret_agg.refundAmount " +
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

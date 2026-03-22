package dao;

import config.DBConnection;
import model.PriceHistory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PriceHistoryDAO – JDBC Data Access Object for the PriceHistory table.
 *
 * Records every price change as an immutable audit row. No updates or
 * deletes are supported – the table is append-only by design.
 */
public class PriceHistoryDAO {

    // ----------------------------------------------------------------
    // SQL constants
    // ----------------------------------------------------------------
    private static final String SQL_INSERT =
            "INSERT INTO PriceHistory (itemCode, oldPrice, newPrice, changedAt, changedBy) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_GET_BY_ITEM =
            "SELECT priceHistoryId, itemCode, oldPrice, newPrice, changedAt, changedBy " +
            "FROM PriceHistory WHERE itemCode = ? ORDER BY changedAt DESC";

    private static final String SQL_GET_BY_ID =
            "SELECT priceHistoryId, itemCode, oldPrice, newPrice, changedAt, changedBy " +
            "FROM PriceHistory WHERE priceHistoryId = ?";

    private static final String SQL_GET_BY_DATE_RANGE =
            "SELECT priceHistoryId, itemCode, oldPrice, newPrice, changedAt, changedBy " +
            "FROM PriceHistory WHERE changedAt BETWEEN ? AND ? ORDER BY changedAt DESC";

    // ================================================================
    // 1. recordPriceChange(PriceHistory record)
    // ================================================================

    /**
     * Appends a new price-change audit record.
     *
     * @param record a fully populated {@link PriceHistory} object
     * @return {@code true} if the record was inserted successfully
     */
    public boolean recordPriceChange(PriceHistory record) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT,
                                         Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, record.getItemCode());
            stmt.setDouble(2, record.getOldPrice());
            stmt.setDouble(3, record.getNewPrice());

            LocalDateTime changedAt = (record.getChangedAt() != null)
                                      ? record.getChangedAt() : LocalDateTime.now();
            stmt.setTimestamp(4, Timestamp.valueOf(changedAt));

            stmt.setString(5, record.getChangedBy());

            success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        record.setPriceHistoryId(keys.getInt(1));
                    }
                }
                System.out.println("Price change recorded: item='" + record.getItemCode()
                                   + "', " + record.getOldPrice() + " → " + record.getNewPrice()
                                   + " by '" + record.getChangedBy() + "'.");
            }

        } catch (SQLException e) {
            System.err.println("recordPriceChange() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 2. getPriceHistoryByItem(String itemCode)
    // ================================================================

    /**
     * Returns the full price-change history for a specific item,
     * most recent first.
     *
     * @param itemCode the item to query
     * @return list of {@link PriceHistory} records; empty if none found
     */
    public List<PriceHistory> getPriceHistoryByItem(String itemCode) {
        List<PriceHistory> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ITEM)) {

            stmt.setString(1, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("getPriceHistoryByItem() failed – " + e.getMessage());
        }

        return list;
    }

    // ================================================================
    // 3. getPriceHistoryById(int priceHistoryId)
    // ================================================================

    /**
     * Retrieves a single audit record by its primary key.
     *
     * @param priceHistoryId the auto-increment PK
     * @return the matching {@link PriceHistory}, or {@code null} if not found
     */
    public PriceHistory getPriceHistoryById(int priceHistoryId) {
        PriceHistory record = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setInt(1, priceHistoryId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    record = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getPriceHistoryById() failed – " + e.getMessage());
        }

        return record;
    }

    // ================================================================
    // 4. getPriceHistoryByDateRange(LocalDateTime start, LocalDateTime end)
    // ================================================================

    /**
     * Returns all price changes that occurred within a date range,
     * useful for manager reporting.
     *
     * @param start range start (inclusive)
     * @param end   range end (inclusive)
     * @return list of {@link PriceHistory} records; empty if none found
     */
    public List<PriceHistory> getPriceHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        List<PriceHistory> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_DATE_RANGE)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("getPriceHistoryByDateRange() failed – " + e.getMessage());
        }

        return list;
    }

    // ================================================================
    // Private helper – maps a ResultSet row to a PriceHistory object
    // ================================================================
    private PriceHistory mapRow(ResultSet rs) throws SQLException {
        PriceHistory record = new PriceHistory();
        record.setPriceHistoryId(rs.getInt("priceHistoryId"));
        record.setItemCode(rs.getString("itemCode"));
        record.setOldPrice(rs.getDouble("oldPrice"));
        record.setNewPrice(rs.getDouble("newPrice"));

        Timestamp ts = rs.getTimestamp("changedAt");
        if (ts != null) {
            record.setChangedAt(ts.toLocalDateTime());
        }

        record.setChangedBy(rs.getString("changedBy"));
        return record;
    }
}

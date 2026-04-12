package dao;

import config.DBConnection;
import model.InventoryRecord;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class InventoryRecordDAO {

    private static final String SQL_INSERT =
            "INSERT INTO InventoryRecord (itemCode, stockLevel, lastUpdated, updatedBy) " +
            "VALUES (?, ?, NOW(), ?)";

    private static final String SQL_GET_BY_ITEM =
            "SELECT inventoryId, itemCode, stockLevel, lastUpdated, updatedBy " +
            "FROM InventoryRecord WHERE itemCode = ?";

    private static final String SQL_SET_STOCK =
            "UPDATE InventoryRecord SET stockLevel=?, lastUpdated=NOW(), updatedBy=? WHERE itemCode=?";

    private static final String SQL_UPDATE_STOCK =
            "UPDATE InventoryRecord " +
            "SET stockLevel = stockLevel + ?, lastUpdated=NOW(), updatedBy=? " +
            "WHERE itemCode=? AND stockLevel + ? >= 0";

    private static final String SQL_GET_STOCK =
            "SELECT stockLevel FROM InventoryRecord WHERE itemCode=?";

    public boolean addInventoryRecord(InventoryRecord record) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setString(1, record.getItemCode());
            stmt.setInt(2, record.getStockLevel());
            stmt.setString(3, record.getUpdatedBy());

            success = stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("addInventoryRecord() failed – " + e.getMessage());
        }

        return success;
    }

    public int getStockLevel(String itemCode) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_STOCK)) {

            stmt.setString(1, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stockLevel");
                }
            }

        } catch (SQLException e) {
            System.err.println("getStockLevel() failed – " + e.getMessage());
        }

        return -1;
    }

    public boolean updateStock(String itemCode, int quantityChange, String updatedBy) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STOCK)) {

            stmt.setInt(1, quantityChange);
            stmt.setString(2, updatedBy);
            stmt.setString(3, itemCode);
            stmt.setInt(4, quantityChange);

            success = stmt.executeUpdate() > 0;

            if (!success) {
                System.out.println("updateStock() failed – insufficient stock or item missing.");
            }

        } catch (SQLException e) {
            System.err.println("updateStock() failed – " + e.getMessage());
        }

        return success;
    }

    public boolean setStockLevel(String itemCode, int newStock, String updatedBy) {
        if (newStock < 0) {
            System.out.println("Stock cannot be negative.");
            return false;
        }

        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SET_STOCK)) {

            stmt.setInt(1, newStock);
            stmt.setString(2, updatedBy);
            stmt.setString(3, itemCode);

            success = stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("setStockLevel() failed – " + e.getMessage());
        }

        return success;
    }

    public InventoryRecord getInventoryByItemCode(String itemCode) {
        InventoryRecord record = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ITEM)) {

            stmt.setString(1, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    record = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getInventoryByItemCode() failed – " + e.getMessage());
        }

        return record;
    }

    public List<InventoryRecord> getAllInventoryRecords() {
        List<InventoryRecord> list = new ArrayList<>();

        String sql = "SELECT inventoryId, itemCode, stockLevel, lastUpdated, updatedBy " +
                     "FROM InventoryRecord ORDER BY itemCode";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("getAllInventoryRecords() failed – " + e.getMessage());
        }

        return list;
    }

    public List<model.InventoryVendorStatus> getInventoryStatusWithVendor() {
        List<model.InventoryVendorStatus> list = new ArrayList<>();

        String sql = "SELECT ir.itemCode, i.itemName, ir.stockLevel, v.vendorName, ir.lastUpdated, ir.updatedBy, i.returnDurationDays " +
                     "FROM InventoryRecord ir " +
                     "JOIN Item i ON ir.itemCode = i.itemCode " +
                     "LEFT JOIN Vendor v ON i.vendorId = v.vendorId " +
                     "ORDER BY ir.itemCode";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                model.InventoryVendorStatus status = new model.InventoryVendorStatus();
                status.setItemCode(rs.getString("itemCode"));
                status.setItemName(rs.getString("itemName"));
                status.setStockLevel(rs.getInt("stockLevel"));
                status.setVendorName(rs.getString("vendorName"));
                status.setReturnDurationDays(rs.getInt("returnDurationDays"));
                
                Timestamp ts = rs.getTimestamp("lastUpdated");
                if (ts != null) {
                    status.setLastUpdated(ts.toLocalDateTime());
                }
                
                status.setUpdatedBy(rs.getString("updatedBy"));
                list.add(status);
            }

        } catch (SQLException e) {
            System.err.println("getInventoryStatusWithVendor() failed – " + e.getMessage());
        }

        return list;
    }

    private InventoryRecord mapRow(ResultSet rs) throws SQLException {
        InventoryRecord record = new InventoryRecord();
        record.setInventoryId(rs.getInt("inventoryId"));
        record.setItemCode(rs.getString("itemCode"));
        record.setStockLevel(rs.getInt("stockLevel"));

        Timestamp ts = rs.getTimestamp("lastUpdated");
        if (ts != null) {
            record.setLastUpdated(ts.toLocalDateTime());
        }

        record.setUpdatedBy(rs.getString("updatedBy"));
        return record;
    }
}
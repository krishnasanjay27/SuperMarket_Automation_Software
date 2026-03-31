package dao;

import config.DBConnection;
import config.DBConnection;
import model.Item;
import model.LowStockVendorAlert;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    private static final String SQL_ADD_ITEM =
            "INSERT INTO Item (itemCode, itemName, price, costPrice, reorderLevel, category, vendorId, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_GET_BY_CODE =
            "SELECT itemCode, itemName, price, costPrice, reorderLevel, category, vendorId, created_at " +
            "FROM Item WHERE itemCode = ?";

    private static final String SQL_GET_ALL =
            "SELECT itemCode, itemName, price, costPrice, reorderLevel, category, vendorId, created_at " +
            "FROM Item ORDER BY category, itemName";

    private static final String SQL_UPDATE_PRICE =
            "UPDATE Item SET price = ? WHERE itemCode = ?";

    private static final String SQL_DELETE_ITEM =
            "DELETE FROM Item WHERE itemCode = ?";

    public boolean addItem(Item item) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ADD_ITEM)) {

            stmt.setString(1, item.getItemCode());
            stmt.setString(2, item.getItemName());
            stmt.setDouble(3, item.getPrice());
            stmt.setDouble(4, item.getCostPrice());
            stmt.setInt(5, item.getReorderLevel());

            String category = (item.getCategory() != null && !item.getCategory().isBlank())
                              ? item.getCategory() : "General";
            stmt.setString(6, category);

            if (item.getVendorId() != null) {
                stmt.setInt(7, item.getVendorId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            LocalDateTime createdAt = (item.getCreatedAt() != null)
                                      ? item.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(8, Timestamp.valueOf(createdAt));

            success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("Item '" + item.getItemCode() + "' added successfully.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("addItem() failed – duplicate itemCode: " + item.getItemCode());
        } catch (SQLException e) {
            System.err.println("addItem() failed – " + e.getMessage());
        }

        return success;
    }

    public Item getItemByCode(String itemCode) {
        Item item = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_CODE)) {

            stmt.setString(1, itemCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    item = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getItemByCode() failed – " + e.getMessage());
        }

        return item;
    }

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("getAllItems() failed – " + e.getMessage());
        }

        return items;
    }

    public boolean updateItemPrice(String itemCode, double price) {
        if (price < 0) {
            System.err.println("updateItemPrice() failed – price cannot be negative.");
            return false;
        }

        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PRICE)) {

            stmt.setDouble(1, price);
            stmt.setString(2, itemCode);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("Price for item '" + itemCode + "' updated to " + price + ".");
            } else {
                System.out.println("updateItemPrice() – no item found with code: " + itemCode);
            }

        } catch (SQLException e) {
            System.err.println("updateItemPrice() failed – " + e.getMessage());
        }

        return success;
    }

    public boolean deleteItem(String itemCode) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ITEM)) {

            stmt.setString(1, itemCode);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("Item '" + itemCode + "' deleted successfully.");
            } else {
                System.out.println("deleteItem() – no item found with code: " + itemCode);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("deleteItem() blocked – item '" + itemCode +
                               "' is referenced by existing records. Remove dependencies first.");
        } catch (SQLException e) {
            System.err.println("deleteItem() failed – " + e.getMessage());
        }

        return success;
    }

    public boolean updateItem(Item item) {
        boolean success = false;

        String sql = "UPDATE Item SET itemName=?, price=?, costPrice=?, reorderLevel=?, category=?, vendorId=? WHERE itemCode=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemName());
            stmt.setDouble(2, item.getPrice());
            stmt.setDouble(3, item.getCostPrice());
            stmt.setInt(4, item.getReorderLevel());
            stmt.setString(5, item.getCategory());
            
            if (item.getVendorId() != null) {
                stmt.setInt(6, item.getVendorId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            stmt.setString(7, item.getItemCode());

            success = stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("updateItem() failed – " + e.getMessage());
        }

        return success;
    }

    public List<Item> searchItemsByName(String keyword) {
        List<Item> items = new ArrayList<>();

        String sql = "SELECT itemCode, itemName, price, costPrice, reorderLevel, category, vendorId, created_at " +
                     "FROM Item WHERE itemName LIKE ? ORDER BY itemName";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + keyword + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("searchItemsByName() failed – " + e.getMessage());
        }

        return items;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemCode(rs.getString("itemCode"));
        item.setItemName(rs.getString("itemName"));
        item.setPrice(rs.getDouble("price"));
        item.setCostPrice(rs.getDouble("costPrice"));
        item.setReorderLevel(rs.getInt("reorderLevel"));
        item.setCategory(rs.getString("category"));

        int vendorId = rs.getInt("vendorId");
        item.setVendorId(rs.wasNull() ? null : vendorId);

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            item.setCreatedAt(ts.toLocalDateTime());
        }

        return item;
    }

    public List<LowStockVendorAlert> getLowStockItemsWithVendor() {
        List<LowStockVendorAlert> alerts = new ArrayList<>();
        String sql = "SELECT " +
                     "    i.itemName, " +
                     "    ir.stockLevel, " +
                     "    i.reorderLevel, " +
                     "    v.vendorName, " +
                     "    v.phone " +
                     "FROM Item i " +
                     "JOIN InventoryRecord ir ON i.itemCode = ir.itemCode " +
                     "LEFT JOIN Vendor v ON i.vendorId = v.vendorId " +
                     "WHERE ir.stockLevel < i.reorderLevel";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LowStockVendorAlert alert = new LowStockVendorAlert();
                alert.setItemName(rs.getString("itemName"));
                alert.setStockLevel(rs.getInt("stockLevel"));
                alert.setReorderLevel(rs.getInt("reorderLevel"));
                alert.setVendorName(rs.getString("vendorName"));
                alert.setPhone(rs.getString("phone"));
                alerts.add(alert);
            }
        } catch (SQLException e) {
            System.err.println("getLowStockItemsWithVendor() failed - " + e.getMessage());
        }
        return alerts;
    }
}

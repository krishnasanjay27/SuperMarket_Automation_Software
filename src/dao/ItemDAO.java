package dao;

import config.DBConnection;
import model.Item;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO – JDBC Data Access Object for the Item table.
 *
 * Handles all database operations for the product master catalogue.
 * No UI logic is included.
 */
public class ItemDAO {

    // ----------------------------------------------------------------
    // SQL constants
    // ----------------------------------------------------------------
    private static final String SQL_ADD_ITEM =
            "INSERT INTO Item (itemCode, itemName, price, costPrice, reorderLevel, category, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_GET_BY_CODE =
            "SELECT itemCode, itemName, price, costPrice, reorderLevel, category, created_at " +
            "FROM Item WHERE itemCode = ?";

    private static final String SQL_GET_ALL =
            "SELECT itemCode, itemName, price, costPrice, reorderLevel, category, created_at " +
            "FROM Item ORDER BY category, itemName";

    private static final String SQL_UPDATE_PRICE =
            "UPDATE Item SET price = ? WHERE itemCode = ?";

    private static final String SQL_DELETE_ITEM =
            "DELETE FROM Item WHERE itemCode = ?";

    // ================================================================
    // 1. addItem(Item item)
    // ================================================================

    /**
     * Inserts a new product into the Item master catalogue.
     *
     * @param item a fully populated {@link Item} object
     * @return {@code true} if the record was inserted successfully
     */
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

            LocalDateTime createdAt = (item.getCreatedAt() != null)
                                      ? item.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

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

    // ================================================================
    // 2. getItemByCode(String itemCode)
    // ================================================================

    /**
     * Retrieves a single {@link Item} by its primary key.
     *
     * @param itemCode the item code to look up
     * @return the matching {@link Item}, or {@code null} if not found
     */
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

    // ================================================================
    // 3. getAllItems()
    // ================================================================

    /**
     * Retrieves the full product catalogue, ordered by category then name.
     *
     * @return a {@link List} of all {@link Item} records; empty list if none found
     */
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

    // ================================================================
    // 4. updateItemPrice(String itemCode, double price)
    // ================================================================

    /**
     * Updates the selling price of an existing item.
     *
     * <p>This method only updates the {@code price} column. The caller
     * (typically a Manager-facing service) is responsible for writing a
     * corresponding record to the {@code PriceHistory} table before or after
     * calling this method.</p>
     *
     * @param itemCode the item whose price is being changed
     * @param price    the new selling price (must be &ge; 0)
     * @return {@code true} if exactly one row was updated
     */
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

    // ================================================================
    // 5. deleteItem(String itemCode)
    // ================================================================

    /**
     * Permanently deletes an item from the catalogue.
     *
     * <p><strong>Warning:</strong> deletion is blocked by the database if the
     * item is referenced in {@code TransactionItem}, {@code InventoryRecord},
     * or {@code PriceHistory} (FK RESTRICT). Remove or reassign dependent
     * records first.</p>
     *
     * @param itemCode the item code to delete
     * @return {@code true} if the record was deleted successfully
     */
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

    //6 update item (manager feature)
public boolean updateItem(Item item) {

    boolean success = false;

    String sql = "UPDATE Item SET itemName=?, price=?, costPrice=?, reorderLevel=?, category=? WHERE itemCode=?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, item.getItemName());
        stmt.setDouble(2, item.getPrice());
        stmt.setDouble(3, item.getCostPrice());
        stmt.setInt(4, item.getReorderLevel());
        stmt.setString(5, item.getCategory());
        stmt.setString(6, item.getItemCode());

        success = stmt.executeUpdate() > 0;

    } catch (SQLException e) {
        System.err.println("updateItem() failed – " + e.getMessage());
    }

    return success;
}

//7 search items by name 
public List<Item> searchItemsByName(String keyword) {

    List<Item> items = new ArrayList<>();

    String sql = "SELECT * FROM Item WHERE itemName LIKE ? ORDER BY itemName";

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
    // ================================================================
    // Private helper – maps a ResultSet row to an Item object
    // ================================================================
    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemCode(rs.getString("itemCode"));
        item.setItemName(rs.getString("itemName"));
        item.setPrice(rs.getDouble("price"));
        item.setCostPrice(rs.getDouble("costPrice"));
        item.setReorderLevel(rs.getInt("reorderLevel"));
        item.setCategory(rs.getString("category"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            item.setCreatedAt(ts.toLocalDateTime());
        }

        return item;
    }
}

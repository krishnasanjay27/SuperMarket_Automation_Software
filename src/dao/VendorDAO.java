package dao;

import config.DBConnection;
import model.Vendor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendorDAO {

    private static final String SQL_INSERT =
            "INSERT INTO Vendor (vendorName, phone, email, address) VALUES (?, ?, ?, ?)";

    private static final String SQL_GET_ALL =
            "SELECT vendorId, vendorName, phone, email, address FROM Vendor ORDER BY vendorName";

    private static final String SQL_GET_BY_ID =
            "SELECT vendorId, vendorName, phone, email, address FROM Vendor WHERE vendorId = ?";

    private static final String SQL_UPDATE =
            "UPDATE Vendor SET phone = ?, email = ?, address = ? WHERE vendorId = ?";

    private static final String SQL_DELETE =
            "DELETE FROM Vendor WHERE vendorId = ?";

    private static final String SQL_COUNT_ITEMS =
            "SELECT COUNT(*) FROM Item WHERE vendorId = ?";

    public boolean insertVendor(Vendor vendor) {
        boolean success = false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, vendor.getVendorName());
            stmt.setString(2, vendor.getPhone());
            stmt.setString(3, vendor.getEmail());
            stmt.setString(4, vendor.getAddress());

            success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        vendor.setVendorId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("insertVendor() failed - Phone number already exists: " + vendor.getPhone());
        } catch (SQLException e) {
            System.err.println("insertVendor() failed - " + e.getMessage());
        }
        return success;
    }

    public List<Vendor> getAllVendors() {
        List<Vendor> vendors = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                vendors.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getAllVendors() failed - " + e.getMessage());
        }
        return vendors;
    }

    public Vendor getVendorById(int vendorId) {
        Vendor vendor = null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    vendor = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("getVendorById() failed - " + e.getMessage());
        }
        return vendor;
    }

    public boolean updateVendor(Vendor vendor) {
        boolean success = false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, vendor.getPhone());
            stmt.setString(2, vendor.getEmail());
            stmt.setString(3, vendor.getAddress());
            stmt.setInt(4, vendor.getVendorId());

            success = stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateVendor() failed - " + e.getMessage());
        }
        return success;
    }

    public int countLinkedItems(int vendorId) {
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_ITEMS)) {

            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("countLinkedItems() failed - " + e.getMessage());
        }
        return count;
    }

    public boolean deleteVendor(int vendorId) {
        boolean success = false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setInt(1, vendorId);
            success = stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("deleteVendor() failed - " + e.getMessage());
        }
        return success;
    }

    private Vendor mapRow(ResultSet rs) throws SQLException {
        Vendor v = new Vendor();
        v.setVendorId(rs.getInt("vendorId"));
        v.setVendorName(rs.getString("vendorName"));
        v.setPhone(rs.getString("phone"));
        v.setEmail(rs.getString("email"));
        v.setAddress(rs.getString("address"));
        return v;
    }
}

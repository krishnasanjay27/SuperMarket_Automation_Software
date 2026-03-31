package dao;

import config.DBConnection;
import model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static final String SQL_INSERT =
            "INSERT INTO Customer (name, phone, email, address, loyaltyPoints) " +
            "VALUES (?, ?, ?, ?, 0)";

    private static final String SQL_GET_BY_PHONE =
            "SELECT customerId, name, phone, email, address, loyaltyPoints " +
            "FROM Customer WHERE phone = ?";

    private static final String SQL_GET_BY_ID =
            "SELECT customerId, name, phone, email, address, loyaltyPoints " +
            "FROM Customer WHERE customerId = ?";

    private static final String SQL_UPDATE_POINTS =
            "UPDATE Customer SET loyaltyPoints = ? WHERE customerId = ?";

    private static final String SQL_GET_ALL =
            "SELECT customerId, name, phone, email, address, loyaltyPoints " +
            "FROM Customer ORDER BY name";

    public boolean addCustomer(Customer customer) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT,
                                         Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getAddress());

            success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        customer.setCustomerId(keys.getInt(1));
                    }
                }
                System.out.println("Customer registered: customerId=" + customer.getCustomerId()
                                   + ", phone=" + customer.getPhone());
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("addCustomer() failed – phone already registered: " + customer.getPhone());
        } catch (SQLException e) {
            System.err.println("addCustomer() failed – " + e.getMessage());
        }

        return success;
    }

    public Customer getCustomerByPhone(String phone) {
        Customer customer = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_PHONE)) {

            stmt.setString(1, phone);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    customer = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getCustomerByPhone() failed – " + e.getMessage());
        }

        return customer;
    }

    public Customer getCustomerById(int customerId) {
        Customer customer = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    customer = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getCustomerById() failed – " + e.getMessage());
        }

        return customer;
    }

    public boolean updateLoyaltyPoints(int customerId, int newPoints) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_POINTS)) {

            stmt.setInt(1, newPoints);
            stmt.setInt(2, customerId);

            success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("Loyalty points updated to " + newPoints
                                   + " for customerId=" + customerId);
            } else {
                System.err.println("updateLoyaltyPoints() – no customer found with id: " + customerId);
            }

        } catch (SQLException e) {
            System.err.println("updateLoyaltyPoints() failed – " + e.getMessage());
        }

        return success;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("getAllCustomers() failed – " + e.getMessage());
        }

        return list;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("customerId"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setLoyaltyPoints(rs.getInt("loyaltyPoints"));
        return c;
    }
}

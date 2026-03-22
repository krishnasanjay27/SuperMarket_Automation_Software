package dao;

import config.DBConnection;
import model.UserAccount;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * UserAccountDAO – JDBC Data Access Object for the UserAccount table.
 *
 * All database interaction for users is centralised here.
 * No UI logic is included; callers are responsible for handling return values.
 */
public class UserAccountDAO {

    // ----------------------------------------------------------------
    // SQL constants
    // ----------------------------------------------------------------
    private static final String SQL_VALIDATE_LOGIN =
            "SELECT userId, role, status FROM UserAccount WHERE userId = ? AND password = ?";

    private static final String SQL_CREATE_USER =
            "INSERT INTO UserAccount (userId, password, role, created_at, status) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_GET_BY_ID =
            "SELECT userId, password, role, created_at, status FROM UserAccount WHERE userId = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE UserAccount SET status = ? WHERE userId = ?";

    private static final String SQL_DELETE_USER =
            "DELETE FROM UserAccount WHERE userId = ?";

    // ================================================================
    // 1. validateLogin(String userId, String password)
    // ================================================================

    /**
     * Validates user credentials against the database.
     *
     * <p>The {@code password} parameter must already be BCrypt-hashed by the
     * caller before being passed in – this method performs a direct hash
     * comparison, matching what is stored in the database.</p>
     *
     * @param userId   the user's login ID
     * @param password BCrypt-hashed password to verify
     * @return a populated {@link UserAccount} if credentials match and the
     *         account is ACTIVE; {@code null} otherwise
     */
    public UserAccount validateLogin(String userId, String password) {
        UserAccount user = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_VALIDATE_LOGIN)) {

            stmt.setString(1, userId);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    // Reject INACTIVE accounts even if credentials match.
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        user = new UserAccount();
                        user.setUserId(rs.getString("userId"));
                        user.setRole(rs.getString("role"));
                        user.setStatus(status);
                        // password intentionally omitted from the returned object
                    } else {
                        System.out.println("Login denied: account '" + userId + "' is INACTIVE.");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("validateLogin() failed – " + e.getMessage());
        }

        return user;
    }

    // ================================================================
    // 2. createUser(UserAccount user)
    // ================================================================

    /**
     * Inserts a new user record into the UserAccount table.
     *
     * <p>The {@code user.getPassword()} value MUST be BCrypt-hashed before
     * calling this method.</p>
     *
     * @param user a fully populated {@link UserAccount} object
     * @return {@code true} if the record was inserted successfully
     */
    public boolean createUser(UserAccount user) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CREATE_USER)) {

            stmt.setString(1, user.getUserId());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());

            // Use provided createdAt or fall back to current timestamp.
            LocalDateTime createdAt = (user.getCreatedAt() != null)
                                      ? user.getCreatedAt()
                                      : LocalDateTime.now();
            stmt.setTimestamp(4, Timestamp.valueOf(createdAt));

            String status = (user.getStatus() != null) ? user.getStatus() : "ACTIVE";
            stmt.setString(5, status);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("User '" + user.getUserId() + "' created successfully.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("createUser() failed – duplicate userId: " + user.getUserId());
        } catch (SQLException e) {
            System.err.println("createUser() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 3. getUserById(String userId)
    // ================================================================

    /**
     * Retrieves a single {@link UserAccount} by its primary key.
     *
     * @param userId the user ID to look up
     * @return the matching {@link UserAccount}, or {@code null} if not found
     */
    public UserAccount getUserById(String userId) {
        UserAccount user = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("getUserById() failed – " + e.getMessage());
        }

        return user;
    }

    // ================================================================
    // 4. updateUserStatus(String userId, String status)
    // ================================================================

    /**
     * Updates the status of an existing user account.
     *
     * @param userId the target user ID
     * @param status the new status: {@code "ACTIVE"} or {@code "INACTIVE"}
     * @return {@code true} if exactly one row was updated
     */
    public boolean updateUserStatus(String userId, String status) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            stmt.setString(1, status);
            stmt.setString(2, userId);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("User '" + userId + "' status updated to '" + status + "'.");
            } else {
                System.out.println("updateUserStatus() – no user found with ID: " + userId);
            }

        } catch (SQLException e) {
            System.err.println("updateUserStatus() failed – " + e.getMessage());
        }

        return success;
    }

    // ================================================================
    // 5. deleteUser(String userId)
    // ================================================================

    /**
     * Permanently deletes a user account.
     *
     * <p><strong>Warning:</strong> deletion will be blocked by the database if
     * the user is referenced in SalesTransaction (FK RESTRICT) or Report
     * tables. Deactivate the account via
     * {@link #updateUserStatus(String, String)} instead when in doubt.</p>
     *
     * @param userId the ID of the user to delete
     * @return {@code true} if the record was deleted successfully
     */
    public boolean deleteUser(String userId) {
        boolean success = false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_USER)) {

            stmt.setString(1, userId);

            int rowsAffected = stmt.executeUpdate();
            success = (rowsAffected > 0);

            if (success) {
                System.out.println("User '" + userId + "' deleted successfully.");
            } else {
                System.out.println("deleteUser() – no user found with ID: " + userId);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("deleteUser() blocked – user '" + userId +
                               "' is referenced by other records. Deactivate instead.");
        } catch (SQLException e) {
            System.err.println("deleteUser() failed – " + e.getMessage());
        }

        return success;
    }

    //6 getAllUsers() (Manager Feature Support)

    public List<UserAccount> getAllUsers() {

    List<UserAccount> users = new ArrayList<>();

    String sql = "SELECT userId, password, role, created_at, status FROM UserAccount";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            users.add(mapRow(rs));
        }

    } catch (SQLException e) {
        System.err.println("getAllUsers() failed – " + e.getMessage());
    }

    return users;
}

//7 updateUserRole()(manager feautre )
public boolean updateUserRole(String userId, String role) {

    boolean success = false;

    String sql = "UPDATE UserAccount SET role = ? WHERE userId = ?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, role);
        stmt.setString(2, userId);

        success = stmt.executeUpdate() > 0;

    } catch (SQLException e) {
        System.err.println("updateUserRole() failed – " + e.getMessage());
    }

    return success;
}

    // ================================================================
    // Private helper – maps a ResultSet row to a UserAccount object
    // ================================================================
    private UserAccount mapRow(ResultSet rs) throws SQLException {
        UserAccount user = new UserAccount();
        user.setUserId(rs.getString("userId"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        user.setStatus(rs.getString("status"));
        return user;
    }
}

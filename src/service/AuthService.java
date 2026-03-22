package service;

import dao.UserAccountDAO;
import model.UserAccount;

import java.time.LocalDateTime;

/**
 * AuthService – service layer for user authentication and account management.
 *
 * Delegates all database operations to {@link UserAccountDAO}.
 * Contains no SQL, no UI logic, and no direct JDBC calls.
 *
 * Password hashing note:
 *   The {@link #hashPassword(String)} method is the single hook point for
 *   integrating a BCrypt library (e.g., jBCrypt or Spring Security Crypto).
 *   Currently it is a pass-through; replace the body with a real hash call
 *   once the library is on the classpath. All methods in this class already
 *   route passwords through it, so no other code will need to change.
 */
public class AuthService {

    // ----------------------------------------------------------------
    // DAO dependency
    // ----------------------------------------------------------------
    private final UserAccountDAO userDAO;

    public AuthService() {
        this.userDAO = new UserAccountDAO();
    }

    // ================================================================
    // 1. login(String userId, String password)
    // ================================================================

    /**
     * Authenticates a user with the supplied credentials.
     *
     * <p>The raw password is hashed via {@link #hashPassword(String)} before
     * being passed to the DAO, matching the stored BCrypt hash.</p>
     *
     * <p>INACTIVE accounts are rejected by the DAO layer and this method
     * returns {@code null} in that case.</p>
     *
     * @param userId   the login ID
     * @param password the plain-text password entered by the user
     * @return a {@link UserAccount} (without password field) on success,
     *         or {@code null} if credentials are invalid or account is INACTIVE
     */
    public UserAccount login(String userId, String password) {
        if (userId == null || userId.isBlank()) {
            System.err.println("login() failed – userId must not be empty.");
            return null;
        }
        if (password == null || password.isBlank()) {
            System.err.println("login() failed – password must not be empty.");
            return null;
        }

        String hashedPassword = hashPassword(password);
        UserAccount user = userDAO.validateLogin(userId, hashedPassword);

        if (user != null) {
            System.out.println("Login successful: userId='" + userId + "', role='" + user.getRole() + "'.");
        } else {
            System.out.println("Login failed: invalid credentials or inactive account for userId='" + userId + "'.");
        }

        return user;
    }

    // ================================================================
    // 2. createUser(UserAccount user)
    // ================================================================

    /**
     * Creates a new user account after hashing the supplied password.
     *
     * <p>The caller must populate {@code user.getPassword()} with the
     * plain-text password; this method hashes it before persistence.</p>
     *
     * <p>Status defaults to {@code "ACTIVE"} if not explicitly set.
     * {@code createdAt} defaults to {@link LocalDateTime#now()} if null.</p>
     *
     * @param user a {@link UserAccount} with at least userId, password, and role set
     * @return {@code true} if the user was created successfully
     */
    public boolean createUser(UserAccount user) {
        if (user == null) {
            System.err.println("createUser() failed – user object must not be null.");
            return false;
        }
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            System.err.println("createUser() failed – userId must not be empty.");
            return false;
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            System.err.println("createUser() failed – password must not be empty.");
            return false;
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            System.err.println("createUser() failed – role must not be empty.");
            return false;
        }

        // Validate role value.
        if (!isValidRole(user.getRole())) {
            System.err.println("createUser() failed – invalid role '" + user.getRole()
                               + "'. Must be: SalesStaff | InventoryStaff | Manager.");
            return false;
        }

        // Hash password before storing.
        user.setPassword(hashPassword(user.getPassword()));

        // Apply defaults.
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        return userDAO.createUser(user);
    }

    // ================================================================
    // 3. changeUserStatus(String userId, String status)
    // ================================================================

    /**
     * Activates or deactivates a user account.
     *
     * <p>Prefer deactivation ({@code "INACTIVE"}) over deletion to preserve
     * referential integrity with SalesTransaction and Report records.</p>
     *
     * @param userId the target user ID
     * @param status {@code "ACTIVE"} or {@code "INACTIVE"}
     * @return {@code true} if the status was updated successfully
     */
    public boolean changeUserStatus(String userId, String status) {
        if (userId == null || userId.isBlank()) {
            System.err.println("changeUserStatus() failed – userId must not be empty.");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            System.err.println("changeUserStatus() failed – status must be 'ACTIVE' or 'INACTIVE'.");
            return false;
        }

        // Confirm user exists before attempting update.
        UserAccount existing = userDAO.getUserById(userId);
        if (existing == null) {
            System.err.println("changeUserStatus() failed – no user found with ID: " + userId);
            return false;
        }

        return userDAO.updateUserStatus(userId, status.toUpperCase());
    }

    // ================================================================
    // 4. getUserById(String userId)
    // ================================================================

    /**
     * Retrieves a user account by its primary key.
     *
     * @param userId the userId to look up
     * @return the matching {@link UserAccount}, or {@code null} if not found
     */
    public UserAccount getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            System.err.println("getUserById() failed – userId must not be empty.");
            return null;
        }

        UserAccount user = userDAO.getUserById(userId);

        if (user == null) {
            System.out.println("getUserById() – no user found with ID: " + userId);
        }

        return user;
    }

    // ================================================================
    // Password hashing hook (replace body when BCrypt is available)
    // ================================================================

    /**
     * Hashes a plain-text password for safe storage and comparison.
     *
     * <p><strong>Current state:</strong> pass-through (returns the input
     * unchanged). Replace this body with a real BCrypt call, for example:
     * <pre>
     *   return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
     * </pre>
     * Once replaced, {@code validateLogin} in the DAO must also switch to
     * {@code BCrypt.checkpw(plain, stored)} instead of a direct hash compare.</p>
     *
     * @param plainPassword the raw password
     * @return the hashed password (currently a no-op pass-through)
     */
    private String hashPassword(String plainPassword) {
        // TODO: replace with BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        return plainPassword;
    }

    // ================================================================
    // Private helper – validates role against allowed ENUM values
    // ================================================================
    private boolean isValidRole(String role) {
        return role.equals("SalesStaff")
            || role.equals("InventoryStaff")
            || role.equals("Manager");
    }
}

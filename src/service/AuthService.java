package service;

import dao.UserAccountDAO;
import model.UserAccount;

import java.time.LocalDateTime;

public class AuthService {

    private final UserAccountDAO userDAO;

    public AuthService() {
        this.userDAO = new UserAccountDAO();
    }

    /** Constructor for dependency injection (testing). */
    public AuthService(UserAccountDAO userDAO) {
        this.userDAO = userDAO;
    }

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
        if (!isValidRole(user.getRole())) {
            System.err.println("createUser() failed – invalid role '" + user.getRole()
                               + "'. Must be: SalesStaff | InventoryStaff | Manager.");
            return false;
        }
        user.setPassword(hashPassword(user.getPassword()));
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return userDAO.createUser(user);
    }

    public boolean changeUserStatus(String userId, String status) {
        if (userId == null || userId.isBlank()) {
            System.err.println("changeUserStatus() failed – userId must not be empty.");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            System.err.println("changeUserStatus() failed – status must be 'ACTIVE' or 'INACTIVE'.");
            return false;
        }
        UserAccount existing = userDAO.getUserById(userId);
        if (existing == null) {
            System.err.println("changeUserStatus() failed – no user found with ID: " + userId);
            return false;
        }
        return userDAO.updateUserStatus(userId, status.toUpperCase());
    }

    public java.util.List<UserAccount> getAllUsers() {
        return userDAO.getAllUsers();
    }

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

    private String hashPassword(String plainPassword) {
        return plainPassword;
    }

    private boolean isValidRole(String role) {
        return role.equals("SalesStaff")
            || role.equals("InventoryStaff")
            || role.equals("Manager");
    }
}

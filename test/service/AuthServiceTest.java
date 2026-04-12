package service;

import dao.UserAccountDAO;
import model.UserAccount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * All DAO interactions are mocked – no database required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserAccountDAO userDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDAO);
    }

    // ─── login() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Returns null when userId is null")
        void login_nullUserId_returnsNull() {
            assertNull(authService.login(null, "password"));
            verifyNoInteractions(userDAO);
        }

        @Test
        @DisplayName("Returns null when userId is blank")
        void login_blankUserId_returnsNull() {
            assertNull(authService.login("   ", "password"));
            verifyNoInteractions(userDAO);
        }

        @Test
        @DisplayName("Returns null when password is null")
        void login_nullPassword_returnsNull() {
            assertNull(authService.login("MGR001", null));
            verifyNoInteractions(userDAO);
        }

        @Test
        @DisplayName("Returns null when password is blank")
        void login_blankPassword_returnsNull() {
            assertNull(authService.login("MGR001", "  "));
            verifyNoInteractions(userDAO);
        }

        @Test
        @DisplayName("Returns user on valid credentials")
        void login_validCredentials_returnsUser() {
            UserAccount user = new UserAccount("MGR001", "pwd", "Manager",
                                               LocalDateTime.now(), "ACTIVE");
            when(userDAO.validateLogin("MGR001", "pwd")).thenReturn(user);

            UserAccount result = authService.login("MGR001", "pwd");

            assertNotNull(result);
            assertEquals("Manager", result.getRole());
            verify(userDAO).validateLogin("MGR001", "pwd");
        }

        @Test
        @DisplayName("Returns null on invalid credentials")
        void login_invalidCredentials_returnsNull() {
            when(userDAO.validateLogin("MGR001", "wrong")).thenReturn(null);

            assertNull(authService.login("MGR001", "wrong"));
            verify(userDAO).validateLogin("MGR001", "wrong");
        }
    }

    // ─── createUser() ─────────────────────────────────────────────

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("Returns false when user is null")
        void createUser_nullUser_returnsFalse() {
            assertFalse(authService.createUser(null));
        }

        @Test
        @DisplayName("Returns false when userId is blank")
        void createUser_blankUserId_returnsFalse() {
            UserAccount u = new UserAccount();
            u.setUserId("");
            u.setPassword("pwd");
            u.setRole("Manager");
            assertFalse(authService.createUser(u));
        }

        @Test
        @DisplayName("Returns false when password is blank")
        void createUser_blankPassword_returnsFalse() {
            UserAccount u = new UserAccount();
            u.setUserId("TEST01");
            u.setPassword("");
            u.setRole("Manager");
            assertFalse(authService.createUser(u));
        }

        @Test
        @DisplayName("Returns false when role is blank")
        void createUser_blankRole_returnsFalse() {
            UserAccount u = new UserAccount();
            u.setUserId("TEST01");
            u.setPassword("pwd");
            u.setRole("");
            assertFalse(authService.createUser(u));
        }

        @Test
        @DisplayName("Returns false for invalid role")
        void createUser_invalidRole_returnsFalse() {
            UserAccount u = new UserAccount();
            u.setUserId("TEST01");
            u.setPassword("pwd");
            u.setRole("Admin");
            assertFalse(authService.createUser(u));
        }

        @Test
        @DisplayName("Creates user with valid data and sets default status")
        void createUser_validData_setsDefaultsAndDelegatesToDAO() {
            UserAccount u = new UserAccount();
            u.setUserId("SALES003");
            u.setPassword("secret");
            u.setRole("SalesStaff");

            when(userDAO.createUser(any(UserAccount.class))).thenReturn(true);

            boolean result = authService.createUser(u);

            assertTrue(result);
            assertEquals("ACTIVE", u.getStatus());
            assertNotNull(u.getCreatedAt());
            verify(userDAO).createUser(u);
        }

        @Test
        @DisplayName("Accepts all valid roles: SalesStaff, InventoryStaff, Manager")
        void createUser_validRoles_accepted() {
            for (String role : new String[]{"SalesStaff", "InventoryStaff", "Manager"}) {
                UserAccount u = new UserAccount();
                u.setUserId("U" + role);
                u.setPassword("pwd");
                u.setRole(role);

                when(userDAO.createUser(any(UserAccount.class))).thenReturn(true);
                assertTrue(authService.createUser(u), "Should accept role: " + role);
            }
        }
    }

    // ─── changeUserStatus() ───────────────────────────────────────

    @Nested
    @DisplayName("changeUserStatus()")
    class ChangeUserStatusTests {

        @Test
        @DisplayName("Returns false when userId is null")
        void changeUserStatus_nullUserId_returnsFalse() {
            assertFalse(authService.changeUserStatus(null, "ACTIVE"));
        }

        @Test
        @DisplayName("Returns false for invalid status")
        void changeUserStatus_invalidStatus_returnsFalse() {
            assertFalse(authService.changeUserStatus("MGR001", "BANNED"));
        }

        @Test
        @DisplayName("Returns false when user does not exist")
        void changeUserStatus_userNotFound_returnsFalse() {
            when(userDAO.getUserById("NO_EXIST")).thenReturn(null);
            assertFalse(authService.changeUserStatus("NO_EXIST", "INACTIVE"));
        }

        @Test
        @DisplayName("Updates status successfully for existing user")
        void changeUserStatus_validParams_delegatesToDAO() {
            UserAccount existing = new UserAccount("MGR001", "pwd", "Manager",
                                                   LocalDateTime.now(), "ACTIVE");
            when(userDAO.getUserById("MGR001")).thenReturn(existing);
            when(userDAO.updateUserStatus("MGR001", "INACTIVE")).thenReturn(true);

            assertTrue(authService.changeUserStatus("MGR001", "inactive"));
            verify(userDAO).updateUserStatus("MGR001", "INACTIVE");
        }
    }

    // ─── getAllUsers() ────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers() delegates to DAO")
    void getAllUsers_delegatesToDAO() {
        List<UserAccount> users = Arrays.asList(
                new UserAccount("U1", "p", "Manager", LocalDateTime.now(), "ACTIVE"),
                new UserAccount("U2", "p", "SalesStaff", LocalDateTime.now(), "ACTIVE"));
        when(userDAO.getAllUsers()).thenReturn(users);

        List<UserAccount> result = authService.getAllUsers();
        assertEquals(2, result.size());
        verify(userDAO).getAllUsers();
    }

    // ─── getUserById() ────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("Returns null when userId is blank")
        void getUserById_blankId_returnsNull() {
            assertNull(authService.getUserById(""));
            verifyNoInteractions(userDAO);
        }

        @Test
        @DisplayName("Returns null when user not found")
        void getUserById_notFound_returnsNull() {
            when(userDAO.getUserById("NOPE")).thenReturn(null);
            assertNull(authService.getUserById("NOPE"));
        }

        @Test
        @DisplayName("Returns user when found")
        void getUserById_found_returnsUser() {
            UserAccount u = new UserAccount("MGR001", "p", "Manager",
                                            LocalDateTime.now(), "ACTIVE");
            when(userDAO.getUserById("MGR001")).thenReturn(u);

            UserAccount result = authService.getUserById("MGR001");
            assertNotNull(result);
            assertEquals("MGR001", result.getUserId());
        }
    }
}

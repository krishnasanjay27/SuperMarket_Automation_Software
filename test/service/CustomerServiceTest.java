package service;

import dao.CustomerDAO;
import model.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Tests")
class CustomerServiceTest {

    @Mock
    private CustomerDAO customerDAO;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerDAO);
    }

    // ─── Phone validation ─────────────────────────────────────────

    @Nested
    @DisplayName("isValidPhone()")
    class PhoneValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"1234567890", "9876543210", "0000000000"})
        @DisplayName("Valid 10-digit phones are accepted")
        void validPhones_returnTrue(String phone) {
            assertTrue(customerService.isValidPhone(phone));
        }

        @ParameterizedTest
        @ValueSource(strings = {"123", "12345678901", "abcdefghij", "123-456-789", ""})
        @DisplayName("Invalid phone formats are rejected")
        void invalidPhones_returnFalse(String phone) {
            assertFalse(customerService.isValidPhone(phone));
        }

        @Test
        @DisplayName("Null phone is rejected")
        void nullPhone_returnsFalse() {
            assertFalse(customerService.isValidPhone(null));
        }
    }

    // ─── registerCustomerIfNotExists() ────────────────────────────

    @Nested
    @DisplayName("registerCustomerIfNotExists()")
    class RegisterTests {

        @Test
        @DisplayName("Returns null for invalid phone")
        void register_invalidPhone_returnsNull() {
            assertNull(customerService.registerCustomerIfNotExists("123", "Test", null, null));
            verifyNoInteractions(customerDAO);
        }

        @Test
        @DisplayName("Returns existing customer if phone already registered")
        void register_existingCustomer_returnsExisting() {
            Customer existing = new Customer(1, "Jane", "9876543210", null, null, 50);
            when(customerDAO.getCustomerByPhone("9876543210")).thenReturn(existing);

            Customer result = customerService.registerCustomerIfNotExists(
                    "9876543210", "Jane", null, null);

            assertNotNull(result);
            assertEquals(1, result.getCustomerId());
            verify(customerDAO, never()).addCustomer(any());
        }

        @Test
        @DisplayName("Creates new customer when phone is not registered")
        void register_newCustomer_createsAndReturns() {
            when(customerDAO.getCustomerByPhone("1111111111")).thenReturn(null);
            when(customerDAO.addCustomer(any(Customer.class))).thenReturn(true);

            Customer result = customerService.registerCustomerIfNotExists(
                    "1111111111", "John", "john@test.com", "123 Street");

            assertNotNull(result);
            assertEquals("1111111111", result.getPhone());
            assertEquals("John", result.getName());
            verify(customerDAO).addCustomer(any(Customer.class));
        }

        @Test
        @DisplayName("Returns null when DAO insert fails")
        void register_daoFails_returnsNull() {
            when(customerDAO.getCustomerByPhone("2222222222")).thenReturn(null);
            when(customerDAO.addCustomer(any(Customer.class))).thenReturn(false);

            assertNull(customerService.registerCustomerIfNotExists(
                    "2222222222", "Failed", null, null));
        }
    }

    // ─── Loyalty points ───────────────────────────────────────────

    @Nested
    @DisplayName("Loyalty Points Operations")
    class LoyaltyPointsTests {

        @Test
        @DisplayName("getAvailablePoints() returns customer's points")
        void getAvailablePoints_returnsPoints() {
            Customer c = new Customer(1, "Test", "0000000000", null, null, 200);
            when(customerDAO.getCustomerById(1)).thenReturn(c);

            assertEquals(200, customerService.getAvailablePoints(1));
        }

        @Test
        @DisplayName("getAvailablePoints() returns 0 for unknown customer")
        void getAvailablePoints_unknownCustomer_returnsZero() {
            when(customerDAO.getCustomerById(999)).thenReturn(null);
            assertEquals(0, customerService.getAvailablePoints(999));
        }

        @Test
        @DisplayName("hasEnoughPoints() returns true when sufficient")
        void hasEnoughPoints_sufficient_returnsTrue() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 100);
            when(customerDAO.getCustomerById(1)).thenReturn(c);

            assertTrue(customerService.hasEnoughPoints(1, 100));
            assertTrue(customerService.hasEnoughPoints(1, 50));
        }

        @Test
        @DisplayName("hasEnoughPoints() returns false when insufficient")
        void hasEnoughPoints_insufficient_returnsFalse() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 30);
            when(customerDAO.getCustomerById(1)).thenReturn(c);

            assertFalse(customerService.hasEnoughPoints(1, 50));
        }

        @Test
        @DisplayName("hasEnoughPoints() returns false for negative points")
        void hasEnoughPoints_negativePoints_returnsFalse() {
            assertFalse(customerService.hasEnoughPoints(1, -5));
        }

        @Test
        @DisplayName("addLoyaltyPoints() adds to existing points")
        void addLoyaltyPoints_addsCorrectly() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 100);
            when(customerDAO.getCustomerById(1)).thenReturn(c);
            when(customerDAO.updateLoyaltyPoints(1, 150)).thenReturn(true);

            assertTrue(customerService.addLoyaltyPoints(1, 50));
            verify(customerDAO).updateLoyaltyPoints(1, 150);
        }

        @Test
        @DisplayName("addLoyaltyPoints() returns false for zero or negative earned")
        void addLoyaltyPoints_zeroOrNegative_returnsFalse() {
            assertFalse(customerService.addLoyaltyPoints(1, 0));
            assertFalse(customerService.addLoyaltyPoints(1, -10));
        }

        @Test
        @DisplayName("redeemPoints() deducts points correctly")
        void redeemPoints_deductsCorrectly() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 200);
            when(customerDAO.getCustomerById(1)).thenReturn(c);
            when(customerDAO.updateLoyaltyPoints(1, 150)).thenReturn(true);

            assertTrue(customerService.redeemPoints(1, 50));
            verify(customerDAO).updateLoyaltyPoints(1, 150);
        }

        @Test
        @DisplayName("redeemPoints() fails when requested more than available")
        void redeemPoints_insufficientBalance_returnsFalse() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 30);
            when(customerDAO.getCustomerById(1)).thenReturn(c);

            assertFalse(customerService.redeemPoints(1, 50));
            verify(customerDAO, never()).updateLoyaltyPoints(anyInt(), anyInt());
        }

        @Test
        @DisplayName("redeemPoints() returns true for zero redemption")
        void redeemPoints_zero_returnsTrue() {
            assertTrue(customerService.redeemPoints(1, 0));
        }

        @Test
        @DisplayName("deductLoyaltyPoints() floors at zero")
        void deductLoyaltyPoints_floorsAtZero() {
            Customer c = new Customer(1, "T", "0000000000", null, null, 10);
            when(customerDAO.getCustomerById(1)).thenReturn(c);
            when(customerDAO.updateLoyaltyPoints(1, 0)).thenReturn(true);

            assertTrue(customerService.deductLoyaltyPoints(1, 50));
            verify(customerDAO).updateLoyaltyPoints(1, 0);
        }
    }

    // ─── Calculation methods ──────────────────────────────────────

    @Nested
    @DisplayName("Calculation Methods")
    class CalculationTests {

        @Test
        @DisplayName("calculateEarnedPoints() returns 1 point per ₹100")
        void calculateEarnedPoints_correctlyComputes() {
            assertEquals(0, customerService.calculateEarnedPoints(0));
            assertEquals(0, customerService.calculateEarnedPoints(99.99));
            assertEquals(1, customerService.calculateEarnedPoints(100.0));
            assertEquals(7, customerService.calculateEarnedPoints(792.98));
            assertEquals(10, customerService.calculateEarnedPoints(1000.0));
        }

        @Test
        @DisplayName("calculateDiscount() returns point value × 1.0")
        void calculateDiscount_correctlyComputes() {
            assertEquals(0.0, customerService.calculateDiscount(0), 0.001);
            assertEquals(50.0, customerService.calculateDiscount(50), 0.001);
            assertEquals(100.0, customerService.calculateDiscount(100), 0.001);
        }
    }

    // ─── getCustomerByPhone / getCustomerById ─────────────────────

    @Test
    @DisplayName("getCustomerByPhone() returns null for invalid phone")
    void getCustomerByPhone_invalidPhone_returnsNull() {
        assertNull(customerService.getCustomerByPhone("abc"));
    }

    @Test
    @DisplayName("getCustomerById() returns null for non-positive ID")
    void getCustomerById_nonPositiveId_returnsNull() {
        assertNull(customerService.getCustomerById(0));
        assertNull(customerService.getCustomerById(-1));
    }

    @Test
    @DisplayName("getAllCustomers() delegates to DAO")
    void getAllCustomers_delegatesToDAO() {
        List<Customer> list = Arrays.asList(new Customer(), new Customer());
        when(customerDAO.getAllCustomers()).thenReturn(list);

        assertEquals(2, customerService.getAllCustomers().size());
        verify(customerDAO).getAllCustomers();
    }
}

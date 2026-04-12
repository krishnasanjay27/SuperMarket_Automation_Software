package service;

import dao.VendorDAO;
import model.Vendor;

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
 * Unit tests for {@link VendorService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService Tests")
class VendorServiceTest {

    @Mock
    private VendorDAO vendorDAO;

    private VendorService vendorService;

    @BeforeEach
    void setUp() {
        vendorService = new VendorService(vendorDAO);
    }

    // ─── isValidPhone() ───────────────────────────────────────────

    @Nested
    @DisplayName("isValidPhone()")
    class PhoneValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"1234567890", "9876543210"})
        @DisplayName("Valid 10-digit phones are accepted")
        void validPhones_accepted(String phone) {
            assertTrue(vendorService.isValidPhone(phone));
        }

        @ParameterizedTest
        @ValueSource(strings = {"123", "abcdefghij", "", "123456789012"})
        @DisplayName("Invalid phones are rejected")
        void invalidPhones_rejected(String phone) {
            assertFalse(vendorService.isValidPhone(phone));
        }

        @Test
        @DisplayName("Null phone is rejected")
        void nullPhone_rejected() {
            assertFalse(vendorService.isValidPhone(null));
        }
    }

    // ─── addVendor() ──────────────────────────────────────────────

    @Nested
    @DisplayName("addVendor()")
    class AddVendorTests {

        @Test
        @DisplayName("Returns false when vendorName is null")
        void addVendor_nullName_returnsFalse() {
            assertFalse(vendorService.addVendor(null, "1234567890", null, null));
        }

        @Test
        @DisplayName("Returns false when vendorName is blank")
        void addVendor_blankName_returnsFalse() {
            assertFalse(vendorService.addVendor("  ", "1234567890", null, null));
        }

        @Test
        @DisplayName("Returns false for invalid phone")
        void addVendor_invalidPhone_returnsFalse() {
            assertFalse(vendorService.addVendor("TestVendor", "123", null, null));
        }

        @Test
        @DisplayName("Adds vendor on success")
        void addVendor_success() {
            when(vendorDAO.insertVendor(any(Vendor.class))).thenReturn(true);

            assertTrue(vendorService.addVendor("FreshFarms", "9876543210",
                                               "fresh@farms.com", "123 Farm Rd"));
            verify(vendorDAO).insertVendor(argThat(v ->
                    v.getVendorName().equals("FreshFarms")
                    && v.getPhone().equals("9876543210")));
        }

        @Test
        @DisplayName("Trims name, email, and address")
        void addVendor_trimsFields() {
            when(vendorDAO.insertVendor(any(Vendor.class))).thenReturn(true);

            assertTrue(vendorService.addVendor("  FreshFarms  ", "9876543210",
                                               "  fresh@farms.com  ", "  123 Farm Rd  "));
            verify(vendorDAO).insertVendor(argThat(v ->
                    v.getVendorName().equals("FreshFarms")
                    && v.getEmail().equals("fresh@farms.com")
                    && v.getAddress().equals("123 Farm Rd")));
        }
    }

    // ─── updateVendor() ───────────────────────────────────────────

    @Nested
    @DisplayName("updateVendor()")
    class UpdateVendorTests {

        @Test
        @DisplayName("Returns false for invalid phone")
        void updateVendor_invalidPhone_returnsFalse() {
            assertFalse(vendorService.updateVendor(1, "123", null, null));
        }

        @Test
        @DisplayName("Returns false when vendor not found")
        void updateVendor_notFound_returnsFalse() {
            when(vendorDAO.getVendorById(999)).thenReturn(null);
            assertFalse(vendorService.updateVendor(999, "1234567890", null, null));
        }

        @Test
        @DisplayName("Updates vendor on success")
        void updateVendor_success() {
            Vendor existing = new Vendor(1, "OldVendor", "1111111111", "old@v.com", "Old Addr");
            when(vendorDAO.getVendorById(1)).thenReturn(existing);
            when(vendorDAO.updateVendor(any(Vendor.class))).thenReturn(true);

            assertTrue(vendorService.updateVendor(1, "2222222222", "new@v.com", "New Addr"));
            verify(vendorDAO).updateVendor(argThat(v ->
                    v.getPhone().equals("2222222222")
                    && v.getEmail().equals("new@v.com")));
        }
    }

    // ─── deleteVendor() ───────────────────────────────────────────

    @Nested
    @DisplayName("deleteVendor()")
    class DeleteVendorTests {

        @Test
        @DisplayName("Returns false when vendor has linked items")
        void deleteVendor_linkedItems_returnsFalse() {
            when(vendorDAO.countLinkedItems(1)).thenReturn(3);
            assertFalse(vendorService.deleteVendor(1));
            verify(vendorDAO, never()).deleteVendor(anyInt());
        }

        @Test
        @DisplayName("Deletes vendor when no linked items")
        void deleteVendor_noLinkedItems_success() {
            when(vendorDAO.countLinkedItems(1)).thenReturn(0);
            when(vendorDAO.deleteVendor(1)).thenReturn(true);

            assertTrue(vendorService.deleteVendor(1));
            verify(vendorDAO).deleteVendor(1);
        }
    }

    // ─── getAllVendors() ──────────────────────────────────────────

    @Test
    @DisplayName("getAllVendors() delegates to DAO")
    void getAllVendors_delegatesToDAO() {
        List<Vendor> vendors = Arrays.asList(new Vendor(), new Vendor());
        when(vendorDAO.getAllVendors()).thenReturn(vendors);

        assertEquals(2, vendorService.getAllVendors().size());
        verify(vendorDAO).getAllVendors();
    }
}

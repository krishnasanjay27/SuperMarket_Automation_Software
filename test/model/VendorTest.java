package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Vendor} model class.
 */
@DisplayName("Vendor Model Tests")
class VendorTest {

    @Test
    @DisplayName("No-arg constructor creates Vendor with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        Vendor v = new Vendor();

        assertEquals(0, v.getVendorId());
        assertNull(v.getVendorName());
        assertNull(v.getPhone());
        assertNull(v.getEmail());
        assertNull(v.getAddress());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        Vendor v = new Vendor(1, "FreshFarms", "9876543210",
                              "fresh@farms.com", "123 Farm Rd");

        assertEquals(1, v.getVendorId());
        assertEquals("FreshFarms", v.getVendorName());
        assertEquals("9876543210", v.getPhone());
        assertEquals("fresh@farms.com", v.getEmail());
        assertEquals("123 Farm Rd", v.getAddress());
    }

    @Test
    @DisplayName("Setters update all fields")
    void setters_updateFieldsCorrectly() {
        Vendor v = new Vendor();
        v.setVendorId(7);
        v.setVendorName("TestVendor");
        v.setPhone("1111111111");
        v.setEmail("test@vendor.com");
        v.setAddress("456 Test Ave");

        assertEquals(7, v.getVendorId());
        assertEquals("TestVendor", v.getVendorName());
        assertEquals("1111111111", v.getPhone());
        assertEquals("test@vendor.com", v.getEmail());
        assertEquals("456 Test Ave", v.getAddress());
    }

    @Test
    @DisplayName("toString() shows vendorId and vendorName")
    void toString_containsIdAndName() {
        Vendor v = new Vendor(3, "AcmeSupplies", "2222222222", null, null);
        String str = v.toString();

        assertTrue(str.contains("3"));
        assertTrue(str.contains("AcmeSupplies"));
    }
}

package service;

import dao.VendorDAO;
import model.Vendor;

import java.util.List;

public class VendorService {

    private static final String PHONE_PATTERN = "\\d{10}";
    private final VendorDAO vendorDAO;

    public VendorService() {
        this.vendorDAO = new VendorDAO();
    }

    public boolean addVendor(String vendorName, String phone, String email, String address) {
        if (vendorName == null || vendorName.isBlank()) {
            System.err.println("addVendor() failed - vendorName is required.");
            return false;
        }
        if (!isValidPhone(phone)) {
            System.err.println("addVendor() failed - phone must be exactly 10 digits.");
            return false;
        }

        Vendor v = new Vendor();
        v.setVendorName(vendorName.trim());
        v.setPhone(phone.trim());
        v.setEmail(email != null ? email.trim() : "");
        v.setAddress(address != null ? address.trim() : "");

        return vendorDAO.insertVendor(v);
    }

    public boolean updateVendor(int vendorId, String phone, String email, String address) {
        if (!isValidPhone(phone)) {
            System.err.println("updateVendor() failed - phone must be exactly 10 digits.");
            return false;
        }

        Vendor v = vendorDAO.getVendorById(vendorId);
        if (v == null) {
            System.err.println("updateVendor() failed - vendor not found: " + vendorId);
            return false;
        }

        v.setPhone(phone.trim());
        v.setEmail(email != null ? email.trim() : "");
        v.setAddress(address != null ? address.trim() : "");

        return vendorDAO.updateVendor(v);
    }

    public boolean deleteVendor(int vendorId) {
        int linkedItems = vendorDAO.countLinkedItems(vendorId);
        if (linkedItems > 0) {
            System.err.println("deleteVendor() rejected - Vendor is linked to " + linkedItems + " Item(s).");
            return false;
        }
        return vendorDAO.deleteVendor(vendorId);
    }

    public List<Vendor> getAllVendors() {
        return vendorDAO.getAllVendors();
    }

    public boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches(PHONE_PATTERN);
    }
}

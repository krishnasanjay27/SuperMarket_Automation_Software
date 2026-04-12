package service;

import dao.CustomerDAO;
import model.Customer;

import java.util.List;

public class CustomerService {

    private static final double POINT_VALUE   = 1.0;
    private static final String PHONE_PATTERN = "\\d{10}";

    private final CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
    }

    /** Constructor for dependency injection (testing). */
    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public Customer registerCustomerIfNotExists(String phone, String name,
                                                String email, String address) {
        if (!isValidPhone(phone)) {
            System.err.println("registerCustomerIfNotExists() failed – phone must be exactly 10 digits: '" + phone + "'.");
            return null;
        }

        Customer existing = customerDAO.getCustomerByPhone(phone);
        if (existing != null) {
            System.out.println("Customer already exists for phone: " + phone
                               + " (customerId=" + existing.getCustomerId() + ").");
            return existing;
        }

        Customer customer = new Customer();
        customer.setPhone(phone.trim());
        customer.setName(name != null ? name.trim() : "");
        customer.setEmail(email != null ? email.trim() : "");
        customer.setAddress(address != null ? address.trim() : "");

        boolean saved = customerDAO.addCustomer(customer);
        return saved ? customer : null;
    }

    public Customer getCustomerByPhone(String phone) {
        if (!isValidPhone(phone)) return null;
        return customerDAO.getCustomerByPhone(phone);
    }

    public Customer getCustomerById(int customerId) {
        if (customerId <= 0) return null;
        return customerDAO.getCustomerById(customerId);
    }

    public int getAvailablePoints(int customerId) {
        Customer c = customerDAO.getCustomerById(customerId);
        return (c != null) ? c.getLoyaltyPoints() : 0;
    }

    public boolean hasEnoughPoints(int customerId, int requestedPoints) {
        if (requestedPoints < 0) return false;
        return requestedPoints <= getAvailablePoints(customerId);
    }

    public boolean addLoyaltyPoints(int customerId, int earnedPoints) {
        if (earnedPoints <= 0) return false;
        int current = getAvailablePoints(customerId);
        return customerDAO.updateLoyaltyPoints(customerId, current + earnedPoints);
    }

    public boolean redeemPoints(int customerId, int pointsToUse) {
        if (pointsToUse <= 0) return true;
        int current = getAvailablePoints(customerId);
        if (pointsToUse > current) {
            System.err.println("redeemPoints() failed – requested " + pointsToUse
                               + " but only " + current + " available.");
            return false;
        }
        return customerDAO.updateLoyaltyPoints(customerId, current - pointsToUse);
    }

    public boolean deductLoyaltyPoints(int customerId, int pointsToDeduct) {
        if (pointsToDeduct <= 0) return true;
        int current = getAvailablePoints(customerId);
        int newPoints = Math.max(0, current - pointsToDeduct);
        return customerDAO.updateLoyaltyPoints(customerId, newPoints);
    }

    public int calculateEarnedPoints(double subtotal) {
        return (int) (subtotal / 100);
    }

    public double calculateDiscount(int pointsUsed) {
        return pointsUsed * POINT_VALUE;
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.getAllCustomers();
    }

    /**
     * Returns true if the phone is exactly 10 numeric digits.
     * This is the single source of phone validation truth for the service layer.
     */
    public boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches(PHONE_PATTERN);
    }
}

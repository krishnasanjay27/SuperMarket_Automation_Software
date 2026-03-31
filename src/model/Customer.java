package model;

public class Customer {

    private int    customerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private int    loyaltyPoints;

    public Customer() { }

    public Customer(int customerId, String name, String phone,
                    String email, String address, int loyaltyPoints) {
        this.customerId    = customerId;
        this.name          = name;
        this.phone         = phone;
        this.email         = email;
        this.address       = address;
        this.loyaltyPoints = loyaltyPoints;
    }

    public int    getCustomerId()                        { return customerId; }
    public void   setCustomerId(int customerId)          { this.customerId = customerId; }

    public String getName()                              { return name; }
    public void   setName(String name)                   { this.name = name; }

    public String getPhone()                             { return phone; }
    public void   setPhone(String phone)                 { this.phone = phone; }

    public String getEmail()                             { return email; }
    public void   setEmail(String email)                 { this.email = email; }

    public String getAddress()                           { return address; }
    public void   setAddress(String address)             { this.address = address; }

    public int    getLoyaltyPoints()                     { return loyaltyPoints; }
    public void   setLoyaltyPoints(int loyaltyPoints)   { this.loyaltyPoints = loyaltyPoints; }

    @Override
    public String toString() {
        return "Customer{" +
               "customerId="      + customerId    +
               ", name='"         + name          + '\'' +
               ", phone='"        + phone         + '\'' +
               ", email='"        + email         + '\'' +
               ", address='"      + address       + '\'' +
               ", loyaltyPoints=" + loyaltyPoints +
               '}';
    }
}

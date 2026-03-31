package model;

public class LowStockVendorAlert {
    private String itemName;
    private int    stockLevel;
    private int    reorderLevel;
    private String vendorName;
    private String phone;

    public LowStockVendorAlert() {
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getStockLevel() { return stockLevel; }
    public void setStockLevel(int stockLevel) { this.stockLevel = stockLevel; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

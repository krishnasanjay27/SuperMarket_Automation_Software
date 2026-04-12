package model;

import java.time.LocalDateTime;

public class InventoryVendorStatus {
    private String itemCode;
    private String itemName;
    private int stockLevel;
    private String vendorName;
    private LocalDateTime lastUpdated;
    private String updatedBy;
    private int returnDurationDays;

    public InventoryVendorStatus() {}

    public int getReturnDurationDays() { return returnDurationDays; }
    public void setReturnDurationDays(int returnDurationDays) { this.returnDurationDays = returnDurationDays; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getStockLevel() { return stockLevel; }
    public void setStockLevel(int stockLevel) { this.stockLevel = stockLevel; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

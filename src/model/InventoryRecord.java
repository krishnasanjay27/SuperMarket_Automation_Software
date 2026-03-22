package model;

import java.time.LocalDateTime;

/**
 * POJO representing the InventoryRecord table.
 * One-to-one relationship with Item. Tracks live stock level.
 */
public class InventoryRecord {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------
    private int           inventoryId;
    private String        itemCode;
    private int           stockLevel;
    private LocalDateTime lastUpdated;
    private String        updatedBy;   // FK → UserAccount.userId (nullable)

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------
    public InventoryRecord() { }

    public InventoryRecord(int inventoryId, String itemCode, int stockLevel,
                           LocalDateTime lastUpdated, String updatedBy) {
        this.inventoryId = inventoryId;
        this.itemCode    = itemCode;
        this.stockLevel  = stockLevel;
        this.lastUpdated = lastUpdated;
        this.updatedBy   = updatedBy;
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------
    public int    getInventoryId()                       { return inventoryId; }
    public void   setInventoryId(int inventoryId)        { this.inventoryId = inventoryId; }

    public String getItemCode()                          { return itemCode; }
    public void   setItemCode(String itemCode)           { this.itemCode = itemCode; }

    public int    getStockLevel()                        { return stockLevel; }
    public void   setStockLevel(int stockLevel)          { this.stockLevel = stockLevel; }

    public LocalDateTime getLastUpdated()                           { return lastUpdated; }
    public void          setLastUpdated(LocalDateTime lastUpdated)  { this.lastUpdated = lastUpdated; }

    public String getUpdatedBy()                         { return updatedBy; }
    public void   setUpdatedBy(String updatedBy)         { this.updatedBy = updatedBy; }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------
    @Override
    public String toString() {
        return "InventoryRecord{" +
               "inventoryId="  + inventoryId +
               ", itemCode='"  + itemCode    + '\'' +
               ", stockLevel=" + stockLevel  +
               ", lastUpdated=" + lastUpdated +
               ", updatedBy='" + updatedBy   + '\'' +
               '}';
    }
}

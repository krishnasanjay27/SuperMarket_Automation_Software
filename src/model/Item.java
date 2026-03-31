package model;

import java.time.LocalDateTime;

public class Item {

    private String        itemCode;
    private String        itemName;
    private double        price;
    private double        costPrice;
    private int           reorderLevel;
    private String        category;
    private LocalDateTime createdAt;
    private Integer       vendorId;

    public Item() { }

    public Item(String itemCode, String itemName, double price, double costPrice,
                int reorderLevel, String category, LocalDateTime createdAt) {
        this.itemCode     = itemCode;
        this.itemName     = itemName;
        this.price        = price;
        this.costPrice    = costPrice;
        this.reorderLevel = reorderLevel;
        this.category     = category;
        this.createdAt    = createdAt;
    }

    public String getItemCode()                       { return itemCode; }
    public void   setItemCode(String itemCode)        { this.itemCode = itemCode; }

    public String getItemName()                       { return itemName; }
    public void   setItemName(String itemName)        { this.itemName = itemName; }

    public double getPrice()                          { return price; }
    public void   setPrice(double price)              { this.price = price; }

    public double getCostPrice()                      { return costPrice; }
    public void   setCostPrice(double costPrice)      { this.costPrice = costPrice; }

    public int    getReorderLevel()                   { return reorderLevel; }
    public void   setReorderLevel(int reorderLevel)   { this.reorderLevel = reorderLevel; }

    public String getCategory()                       { return category; }
    public void   setCategory(String category)        { this.category = category; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getVendorId()                               { return vendorId; }
    public void    setVendorId(Integer vendorId)               { this.vendorId = vendorId; }

    @Override
    public String toString() {
        return "Item{" +
               "itemCode='"    + itemCode    + '\'' +
               ", itemName='"  + itemName    + '\'' +
               ", price="      + price       +
               ", costPrice="  + costPrice   +
               ", reorderLevel=" + reorderLevel +
               ", category='"  + category    + '\'' +
               ", createdAt="  + createdAt   +
               '}';
    }
}

package model;

import java.time.LocalDateTime;

public class PriceHistory {

    private int           priceHistoryId;
    private String        itemCode;
    private double        oldPrice;
    private double        newPrice;
    private LocalDateTime changedAt;
    private String        changedBy;

    public PriceHistory() { }

    public PriceHistory(int priceHistoryId, String itemCode, double oldPrice,
                        double newPrice, LocalDateTime changedAt, String changedBy) {
        this.priceHistoryId = priceHistoryId;
        this.itemCode       = itemCode;
        this.oldPrice       = oldPrice;
        this.newPrice       = newPrice;
        this.changedAt      = changedAt;
        this.changedBy      = changedBy;
    }

    public int    getPriceHistoryId()                      { return priceHistoryId; }
    public void   setPriceHistoryId(int priceHistoryId)   { this.priceHistoryId = priceHistoryId; }

    public String getItemCode()                            { return itemCode; }
    public void   setItemCode(String itemCode)             { this.itemCode = itemCode; }

    public double getOldPrice()                            { return oldPrice; }
    public void   setOldPrice(double oldPrice)             { this.oldPrice = oldPrice; }

    public double getNewPrice()                            { return newPrice; }
    public void   setNewPrice(double newPrice)             { this.newPrice = newPrice; }

    public LocalDateTime getChangedAt()                        { return changedAt; }
    public void          setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getChangedBy()                           { return changedBy; }
    public void   setChangedBy(String changedBy)           { this.changedBy = changedBy; }

    @Override
    public String toString() {
        return "PriceHistory{" +
               "priceHistoryId=" + priceHistoryId +
               ", itemCode='"    + itemCode        + '\'' +
               ", oldPrice="     + oldPrice        +
               ", newPrice="     + newPrice        +
               ", changedAt="    + changedAt       +
               ", changedBy='"   + changedBy       + '\'' +
               '}';
    }
}

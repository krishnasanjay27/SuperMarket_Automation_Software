package model;

import java.time.LocalDateTime;

public class ReturnableItemDTO {

    private String        itemCode;
    private String        itemName;
    private int           purchasedQty;
    private int           alreadyReturnedQty;
    private int           remainingReturnableQty;
    private int           returnDurationDays;
    private String        eligibilityStatus;
    private LocalDateTime transactionDate;
    private double        unitPrice;

    public ReturnableItemDTO() { }

    public String        getItemCode()                                    { return itemCode; }
    public void          setItemCode(String itemCode)                     { this.itemCode = itemCode; }

    public String        getItemName()                                    { return itemName; }
    public void          setItemName(String itemName)                     { this.itemName = itemName; }

    public int           getPurchasedQty()                                { return purchasedQty; }
    public void          setPurchasedQty(int purchasedQty)               { this.purchasedQty = purchasedQty; }

    public int           getAlreadyReturnedQty()                          { return alreadyReturnedQty; }
    public void          setAlreadyReturnedQty(int alreadyReturnedQty)   { this.alreadyReturnedQty = alreadyReturnedQty; }

    public int           getRemainingReturnableQty()                      { return remainingReturnableQty; }
    public void          setRemainingReturnableQty(int qty)               { this.remainingReturnableQty = qty; }

    public int           getReturnDurationDays()                          { return returnDurationDays; }
    public void          setReturnDurationDays(int returnDurationDays)   { this.returnDurationDays = returnDurationDays; }

    public String        getEligibilityStatus()                           { return eligibilityStatus; }
    public void          setEligibilityStatus(String eligibilityStatus)  { this.eligibilityStatus = eligibilityStatus; }

    public LocalDateTime getTransactionDate()                             { return transactionDate; }
    public void          setTransactionDate(LocalDateTime transactionDate){ this.transactionDate = transactionDate; }

    public double        getUnitPrice()                                   { return unitPrice; }
    public void          setUnitPrice(double unitPrice)                   { this.unitPrice = unitPrice; }
}

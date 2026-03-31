package model;

import java.time.LocalDateTime;

public class Bill {

    private int           billId;
    private String        transactionId;
    private LocalDateTime generatedDate;
    private double        totalAmount;
    private int           loyaltyPointsUsed;
    private double        loyaltyDiscount;
    private double        finalTotal;
    private int           loyaltyPointsEarned;

    public Bill() { }

    public Bill(int billId, String transactionId, LocalDateTime generatedDate,
                double totalAmount, int loyaltyPointsUsed, double loyaltyDiscount,
                double finalTotal, int loyaltyPointsEarned) {
        this.billId               = billId;
        this.transactionId        = transactionId;
        this.generatedDate        = generatedDate;
        this.totalAmount          = totalAmount;
        this.loyaltyPointsUsed    = loyaltyPointsUsed;
        this.loyaltyDiscount      = loyaltyDiscount;
        this.finalTotal           = finalTotal;
        this.loyaltyPointsEarned  = loyaltyPointsEarned;
    }

    public int    getBillId()                              { return billId; }
    public void   setBillId(int billId)                   { this.billId = billId; }

    public String getTransactionId()                       { return transactionId; }
    public void   setTransactionId(String transactionId)  { this.transactionId = transactionId; }

    public LocalDateTime getGeneratedDate()                            { return generatedDate; }
    public void          setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }

    public double getTotalAmount()                         { return totalAmount; }
    public void   setTotalAmount(double totalAmount)       { this.totalAmount = totalAmount; }

    public int    getLoyaltyPointsUsed()                   { return loyaltyPointsUsed; }
    public void   setLoyaltyPointsUsed(int pts)            { this.loyaltyPointsUsed = pts; }

    public double getLoyaltyDiscount()                     { return loyaltyDiscount; }
    public void   setLoyaltyDiscount(double discount)      { this.loyaltyDiscount = discount; }

    public double getFinalTotal()                          { return finalTotal; }
    public void   setFinalTotal(double finalTotal)         { this.finalTotal = finalTotal; }

    public int    getLoyaltyPointsEarned()                 { return loyaltyPointsEarned; }
    public void   setLoyaltyPointsEarned(int pts)          { this.loyaltyPointsEarned = pts; }

    @Override
    public String toString() {
        return "Bill{" +
               "billId="                + billId              +
               ", transactionId='"      + transactionId       + '\'' +
               ", generatedDate="       + generatedDate       +
               ", totalAmount="         + totalAmount         +
               ", loyaltyPointsUsed="   + loyaltyPointsUsed   +
               ", loyaltyDiscount="     + loyaltyDiscount     +
               ", finalTotal="          + finalTotal          +
               ", loyaltyPointsEarned=" + loyaltyPointsEarned +
               '}';
    }
}

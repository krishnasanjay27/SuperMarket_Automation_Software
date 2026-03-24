package model;

import java.time.LocalDateTime;

public class Bill {

    private int           billId;
    private String        transactionId;
    private LocalDateTime generatedDate;
    private double        totalAmount;

    public Bill() { }

    public Bill(int billId, String transactionId,
                LocalDateTime generatedDate, double totalAmount) {
        this.billId        = billId;
        this.transactionId = transactionId;
        this.generatedDate = generatedDate;
        this.totalAmount   = totalAmount;
    }

    public int    getBillId()                             { return billId; }
    public void   setBillId(int billId)                  { this.billId = billId; }

    public String getTransactionId()                      { return transactionId; }
    public void   setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getGeneratedDate()                           { return generatedDate; }
    public void          setGeneratedDate(LocalDateTime generatedDate){ this.generatedDate = generatedDate; }

    public double getTotalAmount()                        { return totalAmount; }
    public void   setTotalAmount(double totalAmount)      { this.totalAmount = totalAmount; }

    @Override
    public String toString() {
        return "Bill{" +
               "billId="           + billId        +
               ", transactionId='" + transactionId + '\'' +
               ", generatedDate="  + generatedDate +
               ", totalAmount="    + totalAmount   +
               '}';
    }
}

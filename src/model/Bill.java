package model;

import java.time.LocalDateTime;

/**
 * POJO representing the Bill table.
 * Immutable receipt generated when a SalesTransaction is FINALIZED.
 * One bill per transaction (1-to-1 relationship).
 */
public class Bill {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------
    private int           billId;
    private String        transactionId;   // FK → SalesTransaction.transactionId
    private LocalDateTime generatedDate;
    private double        totalAmount;

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------
    public Bill() { }

    public Bill(int billId, String transactionId,
                LocalDateTime generatedDate, double totalAmount) {
        this.billId        = billId;
        this.transactionId = transactionId;
        this.generatedDate = generatedDate;
        this.totalAmount   = totalAmount;
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------
    public int    getBillId()                              { return billId; }
    public void   setBillId(int billId)                   { this.billId = billId; }

    public String getTransactionId()                       { return transactionId; }
    public void   setTransactionId(String transactionId)  { this.transactionId = transactionId; }

    public LocalDateTime getGeneratedDate()                            { return generatedDate; }
    public void          setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }

    public double getTotalAmount()                         { return totalAmount; }
    public void   setTotalAmount(double totalAmount)       { this.totalAmount = totalAmount; }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------
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

package model;

import java.time.LocalDateTime;

/**
 * POJO representing the SalesTransaction table.
 * (Table was renamed from "Transaction" to avoid MySQL reserved-keyword conflict.)
 *
 * Status values: ACTIVE | FINALIZED | ABORTED
 */
public class SalesTransaction {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------
    private String        transactionId;
    private LocalDateTime transactionDate;
    private double        totalAmount;
    private String        status;        // "ACTIVE" | "FINALIZED" | "ABORTED"
    private String        salesStaffId;  // FK → UserAccount.userId

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------
    public SalesTransaction() { }

    public SalesTransaction(String transactionId, LocalDateTime transactionDate,
                            double totalAmount, String status, String salesStaffId) {
        this.transactionId   = transactionId;
        this.transactionDate = transactionDate;
        this.totalAmount     = totalAmount;
        this.status          = status;
        this.salesStaffId    = salesStaffId;
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------
    public String getTransactionId()                          { return transactionId; }
    public void   setTransactionId(String transactionId)     { this.transactionId = transactionId; }

    public LocalDateTime getTransactionDate()                              { return transactionDate; }
    public void          setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public double getTotalAmount()                            { return totalAmount; }
    public void   setTotalAmount(double totalAmount)          { this.totalAmount = totalAmount; }

    public String getStatus()                                 { return status; }
    public void   setStatus(String status)                    { this.status = status; }

    public String getSalesStaffId()                           { return salesStaffId; }
    public void   setSalesStaffId(String salesStaffId)        { this.salesStaffId = salesStaffId; }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------
    @Override
    public String toString() {
        return "SalesTransaction{" +
               "transactionId='"   + transactionId   + '\'' +
               ", transactionDate=" + transactionDate +
               ", totalAmount="     + totalAmount     +
               ", status='"         + status          + '\'' +
               ", salesStaffId='"   + salesStaffId    + '\'' +
               '}';
    }
}

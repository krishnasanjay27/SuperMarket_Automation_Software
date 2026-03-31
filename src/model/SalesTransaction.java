package model;

import java.time.LocalDateTime;

public class SalesTransaction {

    private String        transactionId;
    private LocalDateTime transactionDate;
    private double        totalAmount;
    private String        status;
    private String        salesStaffId;
    private Integer       customerId;

    public SalesTransaction() { }

    public SalesTransaction(String transactionId, LocalDateTime transactionDate,
                            double totalAmount, String status, String salesStaffId,
                            Integer customerId) {
        this.transactionId   = transactionId;
        this.transactionDate = transactionDate;
        this.totalAmount     = totalAmount;
        this.status          = status;
        this.salesStaffId    = salesStaffId;
        this.customerId      = customerId;
    }

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

    public Integer getCustomerId()                            { return customerId; }
    public void    setCustomerId(Integer customerId)          { this.customerId = customerId; }

    @Override
    public String toString() {
        return "SalesTransaction{" +
               "transactionId='"    + transactionId   + '\'' +
               ", transactionDate=" + transactionDate +
               ", totalAmount="     + totalAmount     +
               ", status='"         + status          + '\'' +
               ", salesStaffId='"   + salesStaffId    + '\'' +
               ", customerId="      + customerId      +
               '}';
    }
}

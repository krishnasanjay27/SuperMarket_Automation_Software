package model;

import java.time.LocalDateTime;

public class ReturnTransaction {

    private int           returnId;
    private String        transactionId;
    private String        itemCode;
    private int           quantity;
    private double        refundAmount;
    private LocalDateTime returnDate;
    private String        processedBy;
    private String        reason;

    public ReturnTransaction() { }

    public ReturnTransaction(String transactionId, String itemCode, int quantity,
                             double refundAmount, String processedBy, String reason) {
        this.transactionId = transactionId;
        this.itemCode      = itemCode;
        this.quantity      = quantity;
        this.refundAmount  = refundAmount;
        this.processedBy   = processedBy;
        this.reason        = reason;
    }

    public int           getReturnId()                          { return returnId; }
    public void          setReturnId(int returnId)              { this.returnId = returnId; }

    public String        getTransactionId()                     { return transactionId; }
    public void          setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String        getItemCode()                          { return itemCode; }
    public void          setItemCode(String itemCode)           { this.itemCode = itemCode; }

    public int           getQuantity()                          { return quantity; }
    public void          setQuantity(int quantity)              { this.quantity = quantity; }

    public double        getRefundAmount()                      { return refundAmount; }
    public void          setRefundAmount(double refundAmount)   { this.refundAmount = refundAmount; }

    public LocalDateTime getReturnDate()                        { return returnDate; }
    public void          setReturnDate(LocalDateTime returnDate){ this.returnDate = returnDate; }

    public String        getProcessedBy()                       { return processedBy; }
    public void          setProcessedBy(String processedBy)     { this.processedBy = processedBy; }

    public String        getReason()                            { return reason; }
    public void          setReason(String reason)               { this.reason = reason; }

    @Override
    public String toString() {
        return "ReturnTransaction{" +
               "returnId="       + returnId       +
               ", transactionId='" + transactionId + '\'' +
               ", itemCode='"    + itemCode       + '\'' +
               ", quantity="     + quantity       +
               ", refundAmount=" + refundAmount   +
               ", returnDate="   + returnDate     +
               ", processedBy='" + processedBy    + '\'' +
               ", reason='"      + reason         + '\'' +
               '}';
    }
}

package model;

/**
 * POJO representing the TransactionItem table.
 * Each instance is a single line item (product + quantity) within a SalesTransaction.
 */
public class TransactionItem {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------
    private int    transactionItemId;
    private String transactionId;   // FK → SalesTransaction.transactionId
    private String itemCode;        // FK → Item.itemCode
    private int    quantity;
    private double unitPrice;
    private double lineTotal;       // stored for billing audit (quantity * unitPrice at sale time)

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------
    public TransactionItem() { }

    public TransactionItem(int transactionItemId, String transactionId, String itemCode,
                           int quantity, double unitPrice, double lineTotal) {
        this.transactionItemId = transactionItemId;
        this.transactionId     = transactionId;
        this.itemCode          = itemCode;
        this.quantity          = quantity;
        this.unitPrice         = unitPrice;
        this.lineTotal         = lineTotal;
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------
    public int    getTransactionItemId()                          { return transactionItemId; }
    public void   setTransactionItemId(int transactionItemId)    { this.transactionItemId = transactionItemId; }

    public String getTransactionId()                              { return transactionId; }
    public void   setTransactionId(String transactionId)         { this.transactionId = transactionId; }

    public String getItemCode()                                   { return itemCode; }
    public void   setItemCode(String itemCode)                    { this.itemCode = itemCode; }

    public int    getQuantity()                                   { return quantity; }
    public void   setQuantity(int quantity)                       { this.quantity = quantity; }

    public double getUnitPrice()                                  { return unitPrice; }
    public void   setUnitPrice(double unitPrice)                  { this.unitPrice = unitPrice; }

    public double getLineTotal()                                  { return lineTotal; }
    public void   setLineTotal(double lineTotal)                  { this.lineTotal = lineTotal; }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------
    @Override
    public String toString() {
        return "TransactionItem{" +
               "transactionItemId=" + transactionItemId +
               ", transactionId='"  + transactionId     + '\'' +
               ", itemCode='"       + itemCode          + '\'' +
               ", quantity="        + quantity          +
               ", unitPrice="       + unitPrice         +
               ", lineTotal="       + lineTotal         +
               '}';
    }
}

package service;

import dao.BillDAO;
import dao.SalesTransactionDAO;
import dao.TransactionItemDAO;
import model.Bill;
import model.SalesTransaction;
import model.TransactionItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillService {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BillDAO             billDAO;
    private final SalesTransactionDAO txnDAO;
    private final TransactionItemDAO  txnItemDAO;

    public BillService() {
        this.billDAO    = new BillDAO();
        this.txnDAO     = new SalesTransactionDAO();
        this.txnItemDAO = new TransactionItemDAO();
    }

    /** Constructor for dependency injection (testing). */
    public BillService(BillDAO billDAO, SalesTransactionDAO txnDAO,
                       TransactionItemDAO txnItemDAO) {
        this.billDAO    = billDAO;
        this.txnDAO     = txnDAO;
        this.txnItemDAO = txnItemDAO;
    }

    public Bill generateBill(String transactionId) {
        Bill existing = billDAO.getBillByTransactionId(transactionId);
        if (existing != null) {
            System.out.println("Bill already exists for transaction: " + transactionId
                               + " (billId=" + existing.getBillId() + "). Reprinting.");
            return existing;
        }

        SalesTransaction txn = txnDAO.getTransactionById(transactionId);
        if (txn == null) {
            System.err.println("generateBill() failed – transaction not found: " + transactionId);
            return null;
        }
        if (!"FINALIZED".equalsIgnoreCase(txn.getStatus())) {
            System.err.println("generateBill() failed – transaction '" + transactionId
                               + "' is " + txn.getStatus() + ". Bills can only be generated for FINALIZED transactions.");
            return null;
        }

        Bill bill = new Bill();
        bill.setTransactionId(transactionId);
        bill.setGeneratedDate(LocalDateTime.now());
        bill.setTotalAmount(txn.getTotalAmount());

        boolean saved = billDAO.generateBill(bill);
        return saved ? bill : null;
    }

    public boolean printBillToConsole(String transactionId) {
        Bill bill = generateBill(transactionId);
        if (bill == null) {
            return false;
        }

        SalesTransaction txn = txnDAO.getTransactionById(transactionId);
        List<TransactionItem> items = txnItemDAO.getItemsByTransactionId(transactionId);

        String divider  = "=".repeat(52);
        String thinLine = "-".repeat(52);

        System.out.println("\n" + divider);
        System.out.println("         SUPERMARKET AUTOMATION SYSTEM         ");
        System.out.println("                  ** RECEIPT **                ");
        System.out.println(divider);
        System.out.printf("  Bill ID      : %d%n",  bill.getBillId());
        System.out.printf("  Transaction  : %s%n",  transactionId);
        System.out.printf("  Date         : %s%n",
            bill.getGeneratedDate() != null ? bill.getGeneratedDate().format(DT_FMT) : "N/A");
        if (txn != null) {
            System.out.printf("  Cashier      : %s%n", txn.getSalesStaffId());
        }
        System.out.println(thinLine);
        System.out.printf("  %-22s %6s %10s %10s%n",
            "Item", "Qty", "UnitPrice", "Total");
        System.out.println(thinLine);

        for (TransactionItem item : items) {
            System.out.printf("  %-22s %6d %10.2f %10.2f%n",
                item.getItemCode(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
        }

        System.out.println(thinLine);
        System.out.printf("  %-38s %10.2f%n", "TOTAL (INR)", bill.getTotalAmount());
        System.out.println(divider);
        System.out.println("        Thank you for shopping with us!        ");
        System.out.println(divider + "\n");

        return true;
    }

    public Bill getBillByTransactionId(String transactionId) {
        return billDAO.getBillByTransactionId(transactionId);
    }
}

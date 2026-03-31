package app.controllers;

import app.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Bill;
import model.Customer;
import model.SalesTransaction;
import model.TransactionItem;
import service.BillService;
import service.CustomerService;
import service.ReportService;
import dao.SalesTransactionDAO;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptController {

    @FXML private Label receiptTxnId;
    @FXML private Label receiptDate;
    @FXML private Label receiptStaffId;
    @FXML private Label receiptStatus;
    @FXML private Label receiptCustomerName;
    @FXML private Label receiptSubtotal;
    @FXML private Label receiptPointsUsed;
    @FXML private Label receiptDiscount;
    @FXML private Label receiptTotal;
    @FXML private Label receiptPointsEarned;

    @FXML private TableView<TransactionItem>            receiptItemsTable;
    @FXML private TableColumn<TransactionItem, String>  rColItemCode;
    @FXML private TableColumn<TransactionItem, Integer> rColQty;
    @FXML private TableColumn<TransactionItem, Double>  rColUnitPrice;
    @FXML private TableColumn<TransactionItem, Double>  rColLineTotal;

    private final ReportService       reportService   = new ReportService();
    private final BillService         billService     = new BillService();
    private final CustomerService     customerService = new CustomerService();
    private final SalesTransactionDAO txnDAO          = new SalesTransactionDAO();

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy  HH:mm");

    @FXML
    public void initialize() {
        rColItemCode .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        rColQty      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        rColUnitPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getUnitPrice()).asObject());
        rColLineTotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getLineTotal()).asObject());
    }

    /**
     * Called by SceneNavigator after the FXML window is created.
     * Populates all receipt fields with data from the database.
     */
    public void loadReceipt(String txnId) {
        // Load transaction meta
        SalesTransaction txn = txnDAO.getTransactionById(txnId);
        if (txn != null) {
            receiptTxnId.setText(txn.getTransactionId());
            receiptDate.setText(txn.getTransactionDate() != null
                    ? txn.getTransactionDate().format(DTF) : "—");
            receiptStaffId.setText(txn.getSalesStaffId());
            receiptStatus.setText(txn.getStatus());

            // Fetch customer by ID (Change 4: use getCustomerById instead of phone)
            Integer customerId = txnDAO.getCustomerId(txnId);
            if (customerId != null && customerId > 0) {
                Customer customer = customerService.getCustomerById(customerId);
                if (customer != null) {
                    receiptCustomerName.setText(customer.getName() + " (" + customer.getPhone() + ")");
                } else {
                    receiptCustomerName.setText("—");
                }
            } else {
                receiptCustomerName.setText("Walk-in Customer");
            }
        } else {
            receiptTxnId.setText(txnId);
            receiptDate.setText("—");
            receiptStaffId.setText(SessionManager.getInstance().getUserId());
            receiptStatus.setText("UNKNOWN");
            receiptCustomerName.setText("—");
        }

        // Load items
        List<TransactionItem> items = reportService.getSalesLineItemsByTransaction(txnId);
        receiptItemsTable.setItems(FXCollections.observableArrayList(items));
        double subtotal = items.stream().mapToDouble(TransactionItem::getLineTotal).sum();
        receiptSubtotal.setText(String.format("₹ %.2f", subtotal));

        // Load bill for loyalty data
        Bill bill = billService.getBillByTransactionId(txnId);
        if (bill != null) {
            receiptPointsUsed.setText(bill.getLoyaltyPointsUsed() + " pts");
            receiptDiscount.setText(String.format("- ₹ %.2f", bill.getLoyaltyDiscount()));
            receiptTotal.setText(String.format("₹ %.2f", bill.getFinalTotal()));
            receiptPointsEarned.setText(bill.getLoyaltyPointsEarned() + " pts");
        } else {
            // Fallback: bill not yet generated or legacy transaction
            receiptPointsUsed.setText("0 pts");
            receiptDiscount.setText("- ₹ 0.00");
            receiptTotal.setText(String.format("₹ %.2f", subtotal));
            receiptPointsEarned.setText("0 pts");
        }
    }

    @FXML
    private void handlePrint() {
        System.out.println("\n========================================");
        System.out.println("       SUPERMARKET AUTOMATION SYSTEM    ");
        System.out.println("             SALES RECEIPT              ");
        System.out.println("========================================");
        System.out.printf("  Transaction : %s%n", receiptTxnId.getText());
        System.out.printf("  Date        : %s%n", receiptDate.getText());
        System.out.printf("  Staff       : %s%n", receiptStaffId.getText());
        System.out.printf("  Status      : %s%n", receiptStatus.getText());
        System.out.printf("  Customer    : %s%n", receiptCustomerName.getText());
        System.out.println("----------------------------------------");
        System.out.printf("  %-14s %6s %10s %10s%n",
                "Item Code", "Qty", "Unit (₹)", "Total (₹)");
        System.out.println("  " + "-".repeat(44));
        for (TransactionItem item : receiptItemsTable.getItems()) {
            System.out.printf("  %-14s %6d %10.2f %10.2f%n",
                    item.getItemCode(), item.getQuantity(),
                    item.getUnitPrice(), item.getLineTotal());
        }
        System.out.println("  " + "=".repeat(44));
        System.out.printf("  %-32s %s%n", "Subtotal:",      receiptSubtotal.getText());
        System.out.printf("  %-32s %s%n", "Points Used:",   receiptPointsUsed.getText());
        System.out.printf("  %-32s %s%n", "Discount:",      receiptDiscount.getText());
        System.out.printf("  %-32s %s%n", "FINAL TOTAL:", receiptTotal.getText());
        System.out.printf("  %-32s %s%n", "Points Earned:", receiptPointsEarned.getText());
        System.out.println("========================================");
        System.out.println("       Thank you for your purchase!     ");
        System.out.println("========================================\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt Printed");
        alert.setHeaderText(null);
        alert.setContentText("Receipt printed to console successfully.");
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) receiptTxnId.getScene().getWindow();
        stage.close();
    }
}

package app.controllers;

import app.AlertHelper;
import app.SceneNavigator;
import app.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.TransactionItem;
import service.TransactionService;
import service.ReportService;

import java.util.List;

public class SalesDashboardController {

    @FXML private Label transactionIdLabel;
    @FXML private Label staffIdLabel;
    @FXML private Label totalLabel;

    @FXML private TableView<TransactionItem>             itemsTable;
    @FXML private TableColumn<TransactionItem, String>   colItemCode;
    @FXML private TableColumn<TransactionItem, Integer>  colQty;
    @FXML private TableColumn<TransactionItem, Double>   colUnitPrice;
    @FXML private TableColumn<TransactionItem, Double>   colLineTotal;

    private final TransactionService transactionService = new TransactionService();
    private final ReportService      reportService      = new ReportService();
    private final SessionManager     session            = SessionManager.getInstance();

    /** Stores the ID of the most recently FINALIZED transaction — used for receipt printing. */
    private String lastFinalizedTxnId = null;

    @FXML
    public void initialize() {
        colItemCode .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        colQty      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        colUnitPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getUnitPrice()).asObject());
        colLineTotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getLineTotal()).asObject());

        if (staffIdLabel != null) {
            staffIdLabel.setText("Logged in as: " + session.getUserId());
        }

        String existing = session.getActiveTransactionId();
        if (existing != null) {
            transactionIdLabel.setText("Active Transaction: " + existing);
            refreshTable();
        }
    }

    @FXML
    private void handleCreateTransaction() {
        String txnId = transactionService.createTransaction(session.getUserId());
        if (txnId == null) {
            AlertHelper.showError("Error", "Failed to create transaction.");
            return;
        }
        session.setActiveTransactionId(txnId);
        lastFinalizedTxnId = null;   // new transaction — clear any previous receipt
        transactionIdLabel.setText("Active Transaction: " + txnId);
        refreshTable();
        AlertHelper.showInfo("Transaction Created", "Transaction ID: " + txnId);
    }

    @FXML
    private void handleAddItem() {
        String txnId = requireActiveTransaction();
        if (txnId == null) return;

        String itemCode = prompt("Add Item", "Enter Item Code:");
        if (itemCode == null) return;

        String qtyStr = prompt("Add Item", "Enter Quantity:");
        if (qtyStr == null) return;

        int qty;
        try { qty = Integer.parseInt(qtyStr.trim()); }
        catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Quantity must be a whole number.");
            return;
        }

        boolean ok = transactionService.addItemToTransaction(txnId, itemCode.trim(), qty);
        if (ok) {
            refreshTable();
            AlertHelper.showInfo("Item Added", "Item '" + itemCode.trim() + "' added to transaction.");
        } else {
            AlertHelper.showError("Error", "Failed to add item. Check item code, quantity, and stock.");
        }
    }

    @FXML
    private void handleUpdateQuantity() {
        TransactionItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a row in the table first.");
            return;
        }

        String newQtyStr = prompt("Update Quantity",
                "Selected: " + selected.getItemCode() + "\nEnter new quantity:");
        if (newQtyStr == null) return;

        int newQty;
        try { newQty = Integer.parseInt(newQtyStr.trim()); }
        catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Quantity must be a whole number.");
            return;
        }

        boolean ok = transactionService.updateItemQuantity(selected.getTransactionItemId(), newQty);
        if (ok) {
            refreshTable();
            AlertHelper.showInfo("Updated", "Quantity updated successfully.");
        } else {
            AlertHelper.showError("Error", "Failed to update quantity.");
        }
    }

    @FXML
    private void handleRemoveItem() {
        TransactionItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a row in the table first.");
            return;
        }

        boolean confirmed = AlertHelper.showConfirm("Remove Item",
                "Remove '" + selected.getItemCode() + "' from the transaction?");
        if (!confirmed) return;

        boolean ok = transactionService.removeItemFromTransaction(selected.getTransactionItemId());
        if (ok) {
            refreshTable();
            AlertHelper.showInfo("Removed", "Item removed from transaction.");
        } else {
            AlertHelper.showError("Error", "Failed to remove item.");
        }
    }

    @FXML
    private void handleFinalizeTransaction() {
        String txnId = requireActiveTransaction();
        if (txnId == null) return;

        // Guard 1: transaction must have at least one item
        if (itemsTable.getItems().isEmpty()) {
            AlertHelper.showError("Empty Transaction",
                    "Cannot finalize an empty transaction.\n"
                    + "Please add at least one item before finalizing.");
            return;
        }

        // Guard 2: total bill amount must be greater than zero
        double total = itemsTable.getItems().stream()
                .mapToDouble(TransactionItem::getLineTotal).sum();
        if (total <= 0.0) {
            AlertHelper.showError("Zero Bill Amount",
                    "The total bill amount is ₹ 0.00.\n"
                    + "A transaction cannot be finalized with a zero total.\n"
                    + "Please check item prices or remove invalid lines.");
            return;
        }

        boolean confirmed = AlertHelper.showConfirm("Finalize Transaction",
                String.format("Finalize transaction %s?\n\nTotal: ₹ %.2f\n\nThis cannot be undone.",
                        txnId, total));
        if (!confirmed) return;

        boolean ok = transactionService.finalizeTransaction(txnId);
        if (ok) {
            lastFinalizedTxnId = txnId;   // save for receipt printing
            session.setActiveTransactionId(null);
            transactionIdLabel.setText("Last Finalized: " + txnId
                    + String.format("  (₹ %.2f) — Ready to print", total));
            itemsTable.setItems(FXCollections.observableArrayList());
            totalLabel.setText("₹ 0.00");
            AlertHelper.showInfo("Transaction Finalized",
                    "Transaction " + txnId + " finalized successfully.\n"
                    + String.format("Grand Total: ₹ %.2f\n\nYou may now print the receipt.", total));
        } else {
            AlertHelper.showError("Error", "Failed to finalize. Ensure the transaction has items.");
        }
    }

    @FXML
    private void handleAbortTransaction() {
        String txnId = requireActiveTransaction();
        if (txnId == null) return;

        boolean confirmed = AlertHelper.showConfirm("Abort Transaction",
                "Abort transaction " + txnId + "? All items will be discarded.");
        if (!confirmed) return;

        boolean ok = transactionService.abortTransaction(txnId);
        if (ok) {
            lastFinalizedTxnId = null;   // aborted — no receipt allowed
            session.setActiveTransactionId(null);
            transactionIdLabel.setText("No active transaction");
            itemsTable.setItems(FXCollections.observableArrayList());
            totalLabel.setText("₹ 0.00");
            AlertHelper.showInfo("Aborted", "Transaction aborted. No receipt will be generated.");
        } else {
            AlertHelper.showError("Error", "Failed to abort transaction.");
        }
    }

    @FXML
    private void handlePrintReceipt() {
        // Block printing if there is still an active (unfinalized) transaction
        if (session.getActiveTransactionId() != null) {
            AlertHelper.showError("Transaction Not Finalized",
                    "The current transaction has not been finalized yet.\n\n"
                    + "Please finalize the transaction before printing the receipt.");
            return;
        }

        // Block printing if no transaction has been finalized yet in this session
        if (lastFinalizedTxnId == null || lastFinalizedTxnId.isBlank()) {
            AlertHelper.showError("No Finalized Transaction",
                    "There is no finalized transaction to print.\n\n"
                    + "Complete and finalize a transaction first.");
            return;
        }

        SceneNavigator.openReceiptWindow(lastFinalizedTxnId);
    }

    @FXML
    private void handleLogout() {
        session.clear();
        SceneNavigator.navigateTo("login.fxml", "Supermarket Automation System – Login");
    }

    private void refreshTable() {
        String txnId = session.getActiveTransactionId();
        if (txnId == null) {
            itemsTable.setItems(FXCollections.observableArrayList());
            totalLabel.setText("₹ 0.00");
            return;
        }
        List<TransactionItem> items = reportService.getSalesLineItemsByTransaction(txnId);
        ObservableList<TransactionItem> obs = FXCollections.observableArrayList(items);
        itemsTable.setItems(obs);

        double total = items.stream().mapToDouble(TransactionItem::getLineTotal).sum();
        totalLabel.setText(String.format("₹ %.2f", total));
    }

    private String requireActiveTransaction() {
        String txnId = session.getActiveTransactionId();
        if (txnId == null || txnId.isBlank()) {
            AlertHelper.showError("No Active Transaction", "Please create a transaction first.");
            return null;
        }
        return txnId;
    }

    private String prompt(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }
}

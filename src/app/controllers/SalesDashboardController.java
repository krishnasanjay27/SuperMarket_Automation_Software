package app.controllers;

import app.AlertHelper;
import app.SceneNavigator;
import app.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import model.Customer;
import model.TransactionItem;
import model.ReturnableItemDTO;
import model.ReturnTransaction;
import service.CustomerService;
import service.TransactionService;
import service.ReportService;
import service.ReturnService;

import java.util.List;
import java.util.Optional;

public class SalesDashboardController {

    @FXML private Label transactionIdLabel;
    @FXML private Label staffIdLabel;
    @FXML private Label totalLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label discountSummaryLabel;

    @FXML private VBox panelTransaction;
    @FXML private VBox panelReturn;

    @FXML private TableView<TransactionItem>             itemsTable;
    @FXML private TableColumn<TransactionItem, String>   colItemCode;
    @FXML private TableColumn<TransactionItem, Integer>  colQty;
    @FXML private TableColumn<TransactionItem, Double>   colUnitPrice;
    @FXML private TableColumn<TransactionItem, Double>   colLineTotal;

    // Customer panel controls
    @FXML private TextField customerPhoneField;
    @FXML private Label     customerNameLabel;
    @FXML private Label     availablePointsLabel;
    @FXML private TextField redeemPointsField;
    @FXML private Label     discountLabel;

    // Return Panel controls
    @FXML private TextField returnTxnIdField;
    
    @FXML private TableView<ReturnableItemDTO>             returnItemsTable;
    @FXML private TableColumn<ReturnableItemDTO, String>   retItemCodeCol;
    @FXML private TableColumn<ReturnableItemDTO, String>   retItemNameCol;
    @FXML private TableColumn<ReturnableItemDTO, Integer>  retPurchasedQtyCol;
    @FXML private TableColumn<ReturnableItemDTO, Integer>  retReturnedQtyCol;
    @FXML private TableColumn<ReturnableItemDTO, Integer>  retRemainingQtyCol;
    @FXML private TableColumn<ReturnableItemDTO, String>   retStatusCol;

    @FXML private VBox returnHistoryBox;
    @FXML private TableView<ReturnTransaction>             returnHistoryTable;
    @FXML private TableColumn<ReturnTransaction, String>   histItemCodeCol;
    @FXML private TableColumn<ReturnTransaction, Integer>  histQtyCol;
    @FXML private TableColumn<ReturnTransaction, Double>   histRefundCol;
    @FXML private TableColumn<ReturnTransaction, Object>   histDateCol;
    @FXML private TableColumn<ReturnTransaction, String>   histReasonCol;

    private final TransactionService transactionService = new TransactionService();
    private final CustomerService    customerService    = new CustomerService();
    private final ReportService      reportService      = new ReportService();
    private final ReturnService      returnService      = new ReturnService();
    private final SessionManager     session            = SessionManager.getInstance();

    private String lastFinalizedTxnId = null;

    @FXML
    public void initialize() {
        colItemCode .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        colQty      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        colUnitPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getUnitPrice()).asObject());
        colLineTotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getLineTotal()).asObject());

        // Setup Return Items Table
        retItemCodeCol    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        retItemNameCol    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        retPurchasedQtyCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getPurchasedQty()).asObject());
        retReturnedQtyCol .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAlreadyReturnedQty()).asObject());
        retRemainingQtyCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRemainingReturnableQty()).asObject());
        retStatusCol      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEligibilityStatus()));

        // Setup Return History Table
        histItemCodeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        histQtyCol     .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        histRefundCol  .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getRefundAmount()).asObject());
        histDateCol    .setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getReturnDate()));
        histReasonCol  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));

        if (staffIdLabel != null) {
            staffIdLabel.setText("Logged in as: " + session.getUserId());
        }

        String existing = session.getActiveTransactionId();
        if (existing != null) {
            transactionIdLabel.setText("Active Transaction: " + existing);
            refreshTable();
        }

        showTransactionPanel();
    }

    @FXML private void showTransactionPanel() {
        if (panelTransaction != null) { panelTransaction.setVisible(true); panelTransaction.setManaged(true); }
        if (panelReturn != null)      { panelReturn.setVisible(false); panelReturn.setManaged(false); }
    }

    @FXML private void showReturnPanel() {
        if (panelTransaction != null) { panelTransaction.setVisible(false); panelTransaction.setManaged(false); }
        if (panelReturn != null)      { panelReturn.setVisible(true); panelReturn.setManaged(true); }
        if (returnHistoryBox != null) { returnHistoryBox.setVisible(false); returnHistoryBox.setManaged(false); }
        if (returnTxnIdField != null) { returnTxnIdField.clear(); }
        if (returnItemsTable != null) { returnItemsTable.setItems(FXCollections.observableArrayList()); }
    }

    @FXML
    private void handleCreateTransaction() {
        // Guard: if there is already an active transaction, ask the user what to do first.
        String existingTxnId = session.getActiveTransactionId();
        if (existingTxnId != null) {
            // Build a custom dialog with three choices.
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Active Transaction Exists");
            alert.setHeaderText("Transaction " + existingTxnId + " is still open.");
            alert.setContentText("You must finalize or abort the current transaction before starting a new one.\n\n"
                    + "What would you like to do?");

            ButtonType btnFinalize = new ButtonType("Finalize Current");
            ButtonType btnAbort    = new ButtonType("Abort Current");
            ButtonType btnCancel   = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnFinalize, btnAbort, btnCancel);

            Optional<ButtonType> choice = alert.showAndWait();
            if (choice.isEmpty() || choice.get() == btnCancel) {
                return; // do nothing — keep the current transaction open
            }

            if (choice.get() == btnFinalize) {
                // Re-use finalize logic but inline the check for empty items
                if (itemsTable.getItems().isEmpty()) {
                    AlertHelper.showError("Cannot Finalize",
                            "The current transaction has no items.\n"
                            + "Please add items before finalizing, or choose Abort instead.");
                    return;
                }
                handleFinalizeTransaction();
                // Only proceed if finalize actually cleared the active transaction
                if (session.getActiveTransactionId() != null) return;

            } else { // Abort
                handleAbortTransaction();
                if (session.getActiveTransactionId() != null) return;
            }
        }

        // No active transaction — safe to create a new one.
        String txnId = transactionService.createTransaction(session.getUserId());
        if (txnId == null) {
            AlertHelper.showError("Error", "Failed to create transaction.");
            return;
        }
        session.setActiveTransactionId(txnId);
        session.setActiveCustomerId(0);
        session.setActiveCustomerPoints(0);
        lastFinalizedTxnId = null;
        transactionIdLabel.setText("Active Transaction: " + txnId);
        clearCustomerPanel();
        refreshTable();
        AlertHelper.showInfo("Transaction Created", "Transaction ID: " + txnId);
    }

    @FXML
    private void handleLoadCustomer() {
        String txnId = requireActiveTransaction();
        if (txnId == null) return;

        String phone = customerPhoneField.getText();
        if (phone == null || phone.isBlank()) {
            AlertHelper.showError("Validation Error", "Please enter a customer phone number.");
            return;
        }
        phone = phone.trim();

        if (!customerService.isValidPhone(phone)) {
            AlertHelper.showError("Invalid Phone Number",
                    "Phone number must be a valid Indian mobile number:\n"
                    + "  • Exactly 10 digits\n"
                    + "  • Must start with 6, 7, 8, or 9\n"
                    + "Entered: \"" + phone + "\"");
            return;
        }

        Customer customer = customerService.getCustomerByPhone(phone);

        if (customer == null) {
            customer = showRegisterDialog(phone);
            if (customer == null) return;
        }

        session.setActiveCustomerId(customer.getCustomerId());
        session.setActiveCustomerPoints(customer.getLoyaltyPoints());
        customerNameLabel.setText(customer.getName().isBlank() ? "—" : customer.getName());
        availablePointsLabel.setText(customer.getLoyaltyPoints() + " pts");
        AlertHelper.showInfo("Customer Loaded",
                "Customer: " + customer.getName() +
                "\nPhone: " + customer.getPhone() +
                "\nLoyalty Points: " + customer.getLoyaltyPoints());
    }

    private Customer showRegisterDialog(String phone) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Phone: " + phone + "\nCustomer not found. Fill in details to register.");

        ButtonType registerBtn = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField    = new TextField();
        nameField.setPromptText("Full Name");
        TextField emailField   = new TextField();
        emailField.setPromptText("Email (optional)");
        TextField addressField = new TextField();
        addressField.setPromptText("Address (optional)");

        grid.add(new Label("Name:"),    0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Email:"),   0, 1); grid.add(emailField,   1, 1);
        grid.add(new Label("Address:"), 0, 2); grid.add(addressField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == registerBtn) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    AlertHelper.showError("Validation Error", "Customer name is required.");
                    return null;
                }
                String email = emailField.getText().trim();
                if (!customerService.isValidEmail(email)) {
                    AlertHelper.showError("Invalid Email",
                            "Please enter a valid email address (e.g. name@example.com)\n"
                            + "or leave it blank if the customer has no email.");
                    return null;
                }
                return customerService.registerCustomerIfNotExists(
                        phone, name, email,
                        addressField.getText().trim());
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            return result.get();
        }
        AlertHelper.showError("Registration Failed", "Customer could not be registered. Please check the entered details.");
        return null;
    }

    @FXML
    private void handleApplyRedemption() {
        int customerId = session.getActiveCustomerId();
        if (customerId <= 0) {
            AlertHelper.showError("No Customer", "Please load a customer before redeeming points.");
            return;
        }

        String input = redeemPointsField.getText();
        if (input == null || input.isBlank()) {
            AlertHelper.showError("Validation Error", "Please enter the number of points to redeem.");
            return;
        }

        int points;
        try {
            points = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Points must be a whole number.");
            return;
        }

        if (points < 0) {
            AlertHelper.showError("Validation Error", "Points cannot be negative.");
            return;
        }

        if (!customerService.hasEnoughPoints(customerId, points)) {
            AlertHelper.showError("Insufficient Points",
                    "Customer only has " + session.getActiveCustomerPoints() + " points.");
            return;
        }

        // Calculate the requested discount and cap it at the current bill subtotal.
        // Only the EFFECTIVE points (those that actually contribute to a discount)
        // should be considered — you cannot deduct more points than the bill allows.
        List<TransactionItem> items = itemsTable.getItems();
        double subtotal = items.stream().mapToDouble(TransactionItem::getLineTotal).sum();

        double requestedDiscount = customerService.calculateDiscount(points);
        double effectiveDiscount = Math.min(requestedDiscount, subtotal);

        // Convert effective discount back to effective points (1 point = ₹1)
        int effectivePoints = (int) Math.ceil(effectiveDiscount);

        if (effectivePoints < points) {
            // Let the user know that fewer points will actually be applied
            AlertHelper.showInfo("Points Capped to Bill Amount",
                    "You entered " + points + " points (₹" + String.format("%.2f", requestedDiscount) + " discount),\n"
                    + "but the bill total is only ₹" + String.format("%.2f", subtotal) + ".\n\n"
                    + "Only " + effectivePoints + " points (₹" + String.format("%.2f", effectiveDiscount) + ") will be applied.\n"
                    + "The remaining " + (points - effectivePoints) + " points will stay in the account.");
            // Update the field to show the effective points that will actually be used
            redeemPointsField.setText(String.valueOf(effectivePoints));
        }

        double finalTotal = subtotal - effectiveDiscount;

        discountLabel.setText("Discount: ₹ " + String.format("%.2f", effectiveDiscount));
        discountSummaryLabel.setText("- ₹ " + String.format("%.2f", effectiveDiscount));
        totalLabel.setText(String.format("₹ %.2f", finalTotal));
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

        if (itemsTable.getItems().isEmpty()) {
            AlertHelper.showError("Empty Transaction",
                    "Cannot finalize an empty transaction.\n"
                    + "Please add at least one item before finalizing.");
            return;
        }

        double subtotal = itemsTable.getItems().stream()
                .mapToDouble(TransactionItem::getLineTotal).sum();
        if (subtotal <= 0.0) {
            AlertHelper.showError("Zero Bill Amount",
                    "The total bill amount is ₹ 0.00.\n"
                    + "A transaction cannot be finalized with a zero total.\n"
                    + "Please check item prices or remove invalid lines.");
            return;
        }

        int pointsToRedeem = 0;
        String redeemText = redeemPointsField.getText();
        if (redeemText != null && !redeemText.isBlank()) {
            try {
                pointsToRedeem = Integer.parseInt(redeemText.trim());
            } catch (NumberFormatException e) {
                AlertHelper.showError("Validation Error", "Points to redeem must be a whole number.");
                return;
            }
        }

        int customerId = session.getActiveCustomerId();
        double discount = (customerId > 0) ? customerService.calculateDiscount(pointsToRedeem) : 0.0;
        if (discount > subtotal) discount = subtotal;
        double finalTotal = subtotal - discount;

        boolean confirmed = AlertHelper.showConfirm("Finalize Transaction",
                String.format("Finalize transaction %s?%n%nSubtotal:  ₹ %.2f%n"
                        + "Discount:  ₹ %.2f%nFinal Total: ₹ %.2f%n%nThis cannot be undone.",
                        txnId, subtotal, discount, finalTotal));
        if (!confirmed) return;

        boolean ok = transactionService.finalizeTransaction(txnId, customerId, pointsToRedeem);
        if (ok) {
            lastFinalizedTxnId = txnId;
            session.setActiveTransactionId(null);
            session.setActiveCustomerId(0);
            session.setActiveCustomerPoints(0);
            transactionIdLabel.setText("Last Finalized: " + txnId
                    + String.format("  (₹ %.2f) — Ready to print", finalTotal));
            itemsTable.setItems(FXCollections.observableArrayList());
            subtotalLabel.setText("₹ 0.00");
            discountSummaryLabel.setText("- ₹ 0.00");
            totalLabel.setText("₹ 0.00");
            clearCustomerPanel();
            AlertHelper.showInfo("Transaction Finalized",
                    "Transaction " + txnId + " finalized.\n"
                    + String.format("Final Total: ₹ %.2f\n\nYou may now print the receipt.", finalTotal));
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
            lastFinalizedTxnId = null;
            session.setActiveTransactionId(null);
            session.setActiveCustomerId(0);
            session.setActiveCustomerPoints(0);
            transactionIdLabel.setText("No active transaction");
            itemsTable.setItems(FXCollections.observableArrayList());
            subtotalLabel.setText("₹ 0.00");
            discountSummaryLabel.setText("- ₹ 0.00");
            totalLabel.setText("₹ 0.00");
            clearCustomerPanel();
            AlertHelper.showInfo("Aborted", "Transaction aborted. No receipt will be generated.");
        } else {
            AlertHelper.showError("Error", "Failed to abort transaction.");
        }
    }

    @FXML
    private void handlePrintReceipt() {
        if (session.getActiveTransactionId() != null) {
            AlertHelper.showError("Transaction Not Finalized",
                    "The current transaction has not been finalized yet.\n\n"
                    + "Please finalize the transaction before printing the receipt.");
            return;
        }

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
            subtotalLabel.setText("₹ 0.00");
            discountSummaryLabel.setText("- ₹ 0.00");
            totalLabel.setText("₹ 0.00");
            return;
        }

        List<TransactionItem> items = reportService.getSalesLineItemsByTransaction(txnId);
        ObservableList<TransactionItem> obs = FXCollections.observableArrayList(items);
        itemsTable.setItems(obs);

        double subtotal = items.stream().mapToDouble(TransactionItem::getLineTotal).sum();
        subtotalLabel.setText(String.format("₹ %.2f", subtotal));

        int points = 0;
        String redeemText = redeemPointsField != null ? redeemPointsField.getText() : "";
        if (redeemText != null && !redeemText.isBlank()) {
            try { points = Integer.parseInt(redeemText.trim()); }
            catch (NumberFormatException ignored) { }
        }
        double discount = (session.getActiveCustomerId() > 0)
                ? Math.min(customerService.calculateDiscount(points), subtotal)
                : 0.0;
        discountSummaryLabel.setText(String.format("- ₹ %.2f", discount));
        totalLabel.setText(String.format("₹ %.2f", subtotal - discount));
    }

    private void clearCustomerPanel() {
        if (customerPhoneField    != null) customerPhoneField.clear();
        if (customerNameLabel     != null) customerNameLabel.setText("—");
        if (availablePointsLabel  != null) availablePointsLabel.setText("—");
        if (redeemPointsField     != null) redeemPointsField.clear();
        if (discountLabel         != null) discountLabel.setText("Discount: ₹ 0.00");
        if (discountSummaryLabel  != null) discountSummaryLabel.setText("- ₹ 0.00");
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

    // ──────────────────────────────────────────
    // RETURN MANAGEMENT HANDLERS
    // ──────────────────────────────────────────

    @FXML
    private void handleLoadReturnItems() {
        String txnId = returnTxnIdField.getText();
        if (txnId == null || txnId.isBlank()) {
            AlertHelper.showError("Validation Error", "Please enter a Transaction ID.");
            return;
        }
        txnId = txnId.trim();

        if (returnService.validateTransaction(txnId) == null) {
            AlertHelper.showError("Invalid Transaction",
                    "Transaction '" + txnId + "' does not exist or is not FINALIZED.");
            return;
        }

        List<ReturnableItemDTO> items = returnService.getReturnableItems(txnId);
        returnItemsTable.setItems(FXCollections.observableArrayList(items));

        if (items.isEmpty()) {
            AlertHelper.showInfo("No Items", "No returnable items found for this transaction.");
        }

        if (returnHistoryBox != null) {
            returnHistoryBox.setVisible(false);
            returnHistoryBox.setManaged(false);
        }
    }

    @FXML
    private void handleProcessReturn() {
        ReturnableItemDTO selected = returnItemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select an item from the table first.");
            return;
        }

        if (!"Returnable".equals(selected.getEligibilityStatus())) {
            String expMsg = "";
            if (selected.getTransactionDate() != null) {
                expMsg = " Transaction Date: " + selected.getTransactionDate().toLocalDate() + 
                         ". Max return days: " + selected.getReturnDurationDays();
            }
            AlertHelper.showError("Not Eligible", "This item is marked as Not Returnable." + expMsg);
            return;
        }

        String txnId = returnTxnIdField.getText().trim();
        String itemCode = selected.getItemCode();
        int remaining = selected.getRemainingReturnableQty();

        String qtyStr = prompt("Process Return", "Returning: " + selected.getItemName() + 
                               "\nRemaining eligible: " + remaining + "\nEnter return quantity:");
        if (qtyStr == null) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyStr.trim());
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Quantity must be a whole number.");
            return;
        }

        if (qty <= 0 || qty > remaining) {
            AlertHelper.showError("Invalid Quantity", "Quantity must be between 1 and " + remaining + ".");
            return;
        }

        // Using ChoiceDialog for predefined reasons
        List<String> reasons = List.of("Damaged", "Wrong Item", "Customer Changed Mind");
        ChoiceDialog<String> reasonDialog = new ChoiceDialog<>("Customer Changed Mind", reasons);
        reasonDialog.setTitle("Return Reason");
        reasonDialog.setHeaderText("Select the reason for returning " + qty + "x " + selected.getItemName());
        reasonDialog.setContentText("Reason:");
        Optional<String> result = reasonDialog.showAndWait();
        if (result.isEmpty()) return;
        
        String reason = result.get();
        String processedBy = session.getUserId();

        boolean ok = returnService.processReturn(txnId, itemCode, qty, processedBy, reason);
        if (ok) {
            double refundAmount = selected.getUnitPrice() * qty;
            AlertHelper.showInfo("Return Successful", 
                "Return Processed Successfully.\n\nItem: " + selected.getItemName() +
                "\nQuantity: " + qty + "\nRefund Equivalent: ₹" + String.format("%.2f", refundAmount) + 
                "\n\n(Refund amount has been credited as Loyalty Store Points if a customer account was linked)");
            // Refresh table
            handleLoadReturnItems();
        } else {
            AlertHelper.showError("Error", "Could not process return. Check database logs.");
        }
    }

    @FXML
    private void handleViewReturnHistory() {
        String txnId = returnTxnIdField.getText();
        if (txnId == null || txnId.isBlank()) {
            AlertHelper.showError("Validation Error", "Please enter a Transaction ID first.");
            return;
        }
        txnId = txnId.trim();

        List<ReturnTransaction> history = returnService.getReturnHistory(txnId);
        returnHistoryTable.setItems(FXCollections.observableArrayList(history));
        
        if (returnHistoryBox != null) {
            returnHistoryBox.setVisible(true);
            returnHistoryBox.setManaged(true);
        }

        if (history.isEmpty()) {
            AlertHelper.showInfo("Return History", "No past returns found for transaction: " + txnId);
        }
    }
}

package app.controllers;

import app.AlertHelper;
import app.SceneNavigator;
import app.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.*;
import service.AuthService;
import service.InventoryService;
import service.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ManagerDashboardController {

    // ── Navigation Panels ─────────────────────────────────────────────────────
    @FXML private Label  managerIdLabel;
    @FXML private VBox   panelWelcome;
    @FXML private VBox   panelAddItem;
    @FXML private VBox   panelUpdatePrice;
    @FXML private VBox   panelInventoryStatus;
    @FXML private VBox   panelLowStock;
    @FXML private VBox   panelTransactions;
    @FXML private VBox   panelPriceHistory;
    @FXML private VBox   panelStaffManagement;

    // ── Add Item Form ─────────────────────────────────────────────────────────
    @FXML private TextField aiItemCode;
    @FXML private TextField aiItemName;
    @FXML private TextField aiPrice;
    @FXML private TextField aiCostPrice;
    @FXML private TextField aiReorderLevel;
    @FXML private TextField aiCategory;
    @FXML private TextField aiInitialStock;

    // ── Update Price Form ─────────────────────────────────────────────────────
    @FXML private TextField upItemCode;
    @FXML private TextField upNewPrice;

    // ── Inventory Status Table ────────────────────────────────────────────────
    @FXML private TableView<InventoryRecord>            inventoryTable;
    @FXML private TableColumn<InventoryRecord, String>  invItemCodeCol;
    @FXML private TableColumn<InventoryRecord, Integer> invStockCol;
    @FXML private TableColumn<InventoryRecord, String>  invUpdatedByCol;
    @FXML private TableColumn<InventoryRecord, Object>  invLastUpdatedCol;

    // ── Low Stock Table ───────────────────────────────────────────────────────
    @FXML private TableView<Item>             lowStockTable;
    @FXML private TableColumn<Item, String>   lsItemCodeCol;
    @FXML private TableColumn<Item, String>   lsItemNameCol;
    @FXML private TableColumn<Item, Integer>  lsReorderCol;
    @FXML private TableColumn<Item, String>   lsCategoryCol;

    // ── Transactions Table ────────────────────────────────────────────────────
    @FXML private TableView<SalesTransaction>            txnTable;
    @FXML private TableColumn<SalesTransaction, String>  txnIdCol;
    @FXML private TableColumn<SalesTransaction, Object>  txnDateCol;
    @FXML private TableColumn<SalesTransaction, Double>  txnAmountCol;
    @FXML private TableColumn<SalesTransaction, String>  txnStatusCol;
    @FXML private TableColumn<SalesTransaction, String>  txnStaffCol;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    // ── Price History ─────────────────────────────────────────────────────────
    @FXML private TextField                              phItemCode;
    @FXML private TableView<PriceHistory>                priceHistTable;
    @FXML private TableColumn<PriceHistory, Double>      phOldPriceCol;
    @FXML private TableColumn<PriceHistory, Double>      phNewPriceCol;
    @FXML private TableColumn<PriceHistory, Object>      phChangedAtCol;
    @FXML private TableColumn<PriceHistory, String>      phChangedByCol;

    // ── Staff Management ──────────────────────────────────────────────────────
    @FXML private TableView<UserAccount>                 staffTable;
    @FXML private TableColumn<UserAccount, String>       stUserIdCol;
    @FXML private TableColumn<UserAccount, String>       stRoleCol;
    @FXML private TableColumn<UserAccount, String>       stStatusCol;
    @FXML private TableColumn<UserAccount, Object>       stCreatedAtCol;
    @FXML private VBox     addStaffForm;
    @FXML private VBox     deactivateForm;
    @FXML private VBox     reactivateForm;
    @FXML private TextField newUserId;
    @FXML private PasswordField newPassword;
    @FXML private ComboBox<String> newRole;
    @FXML private TextField deactivateUserId;
    @FXML private TextField reactivateUserId;

    // ── Services ──────────────────────────────────────────────────────────────
    private final InventoryService inventoryService = new InventoryService();
    private final ReportService    reportService    = new ReportService();
    private final AuthService      authService      = new AuthService();
    private final SessionManager   session          = SessionManager.getInstance();



    @FXML
    public void initialize() {
        if (managerIdLabel != null) {
            managerIdLabel.setText("Logged in as: " + session.getUserId());
        }
        setupTables();
        if (newRole != null) {
            newRole.setItems(FXCollections.observableArrayList(
                    "SalesStaff", "InventoryStaff", "Manager"));
        }
        showPanel(panelWelcome);
    }

    // ── Sidebar Navigation ────────────────────────────────────────────────────
    @FXML private void showAddItem()         { showPanel(panelAddItem); }
    @FXML private void showUpdatePrice()     { showPanel(panelUpdatePrice); }
    @FXML private void showInventoryStatus() { showPanel(panelInventoryStatus); handleViewInventoryStatus(); }
    @FXML private void showLowStock()        { showPanel(panelLowStock); handleViewLowStockItems(); }
    @FXML private void showTransactions()    { showPanel(panelTransactions); }
    @FXML private void showPriceHistory()    { showPanel(panelPriceHistory); }
    @FXML private void showStaffManagement() { showPanel(panelStaffManagement); }

    // ── Add New Item ──────────────────────────────────────────────────────────
    @FXML
    private void handleAddNewItem() {
        String itemCode   = aiItemCode.getText().trim();
        String itemName   = aiItemName.getText().trim();
        String priceStr   = aiPrice.getText().trim();
        String costStr    = aiCostPrice.getText().trim();
        String reorderStr = aiReorderLevel.getText().trim();
        String category   = aiCategory.getText().trim();
        String stockStr   = aiInitialStock.getText().trim();

        if (itemCode.isEmpty() || itemName.isEmpty() || priceStr.isEmpty()
                || costStr.isEmpty() || reorderStr.isEmpty()
                || category.isEmpty() || stockStr.isEmpty()) {
            AlertHelper.showError("Validation Error", "All fields are required.");
            return;
        }

        double price, costPrice;
        int reorderLevel, initialStock;
        try {
            price        = Double.parseDouble(priceStr);
            costPrice    = Double.parseDouble(costStr);
            reorderLevel = Integer.parseInt(reorderStr);
            initialStock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error",
                    "Price/Cost must be decimals; Reorder Level/Stock must be integers.");
            return;
        }

        Item item = new Item(itemCode, itemName, price, costPrice,
                reorderLevel, category, LocalDateTime.now());

        boolean ok = inventoryService.addNewItem(item, initialStock, session.getUserId());
        if (ok) {
            AlertHelper.showInfo("Success", "Item '" + itemCode + "' added with stock " + initialStock + ".");
            handleClearAddItem();
        } else {
            AlertHelper.showError("Error", "Failed to add item. Item code may already exist.");
        }
    }

    @FXML
    private void handleClearAddItem() {
        aiItemCode.clear(); aiItemName.clear(); aiPrice.clear();
        aiCostPrice.clear(); aiReorderLevel.clear(); aiCategory.clear();
        aiInitialStock.clear();
    }

    // ── Update Item Price ─────────────────────────────────────────────────────
    @FXML
    private void handleUpdateItemPrice() {
        String itemCode = upItemCode.getText().trim();
        String priceStr = upNewPrice.getText().trim();

        if (itemCode.isEmpty() || priceStr.isEmpty()) {
            AlertHelper.showError("Validation Error", "Item code and new price are required.");
            return;
        }
        double newPrice;
        try { newPrice = Double.parseDouble(priceStr); }
        catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Price must be a valid decimal.");
            return;
        }

        boolean ok = inventoryService.updateItemPrice(itemCode, newPrice, session.getUserId());
        if (ok) {
            AlertHelper.showInfo("Success", "Price updated for '" + itemCode + "' → ₹" + newPrice);
            upItemCode.clear(); upNewPrice.clear();
        } else {
            AlertHelper.showError("Error", "Failed to update price. Check item code.");
        }
    }

    @FXML
    private void handleClearUpdatePrice() { upItemCode.clear(); upNewPrice.clear(); }

    // ── Inventory Status ──────────────────────────────────────────────────────
    @FXML
    private void handleViewInventoryStatus() {
        List<InventoryRecord> records = reportService.getInventoryStatus();
        inventoryTable.setItems(FXCollections.observableArrayList(records));
        if (records.isEmpty())
            AlertHelper.showInfo("Inventory Status", "No inventory records found.");
    }

    // ── Low Stock ─────────────────────────────────────────────────────────────
    @FXML
    private void handleViewLowStockItems() {
        List<Item> items = reportService.getItemsBelowReorderLevel();
        lowStockTable.setItems(FXCollections.observableArrayList(items));
        if (items.isEmpty())
            AlertHelper.showInfo("Low Stock", "No items are below their reorder level.");
    }

    // ── Transactions by Date ──────────────────────────────────────────────────
    @FXML
    private void handleViewTransactionsByDate() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate   = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            AlertHelper.showError("Validation Error", "Please select both start and end dates.");
            return;
        }
        if (endDate.isBefore(startDate)) {
            AlertHelper.showError("Validation Error", "End date must be after start date.");
            return;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(LocalTime.MAX);

        List<SalesTransaction> txns = reportService.getTransactionsByDateRange(start, end);
        txnTable.setItems(FXCollections.observableArrayList(txns));
        if (txns.isEmpty())
            AlertHelper.showInfo("Transactions", "No transactions found in the selected range.");
    }

    // ── Price History ─────────────────────────────────────────────────────────
    @FXML
    private void handleViewPriceHistory() {
        String itemCode = phItemCode.getText().trim();
        if (itemCode.isEmpty()) {
            AlertHelper.showError("Validation Error", "Item code is required.");
            return;
        }
        List<PriceHistory> history = reportService.getPriceHistory(itemCode);
        priceHistTable.setItems(FXCollections.observableArrayList(history));
        if (history.isEmpty())
            AlertHelper.showInfo("Price History", "No price history found for: " + itemCode);
    }

    // ── Staff Management ──────────────────────────────────────────────────────
    @FXML
    private void handleViewAllStaff() {
        hideStaffForms();
        List<UserAccount> users = authService.getAllUsers();
        staffTable.setItems(FXCollections.observableArrayList(users));
        if (users.isEmpty())
            AlertHelper.showInfo("Staff", "No staff accounts found.");
    }

    @FXML
    private void showAddStaff() {
        hideStaffForms();
        setVisible(addStaffForm, true);
    }

    @FXML
    private void showDeactivateStaff() {
        hideStaffForms();
        setVisible(deactivateForm, true);
    }

    @FXML
    private void showReactivateStaff() {
        hideStaffForms();
        setVisible(reactivateForm, true);
    }

    @FXML
    private void handleAddStaff() {
        String userId   = newUserId.getText().trim();
        String password = newPassword.getText().trim();
        String role     = newRole.getValue();

        if (userId.isEmpty() || password.isEmpty() || role == null) {
            AlertHelper.showError("Validation Error", "All fields including role are required.");
            return;
        }

        UserAccount user = new UserAccount();
        user.setUserId(userId);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        boolean ok = authService.createUser(user);
        if (ok) {
            AlertHelper.showInfo("Success", "Account '" + userId + "' created with role: " + role);
            newUserId.clear(); newPassword.clear(); newRole.setValue(null);
            handleViewAllStaff();
        } else {
            AlertHelper.showError("Error", "Failed to create account. User ID may already exist.");
        }
    }

    @FXML
    private void handleDeactivateStaff() {
        String userId = deactivateUserId.getText().trim();
        if (userId.isEmpty()) {
            AlertHelper.showError("Validation Error", "User ID is required.");
            return;
        }
        boolean ok = authService.changeUserStatus(userId, "INACTIVE");
        if (ok) {
            AlertHelper.showInfo("Success", "Account '" + userId + "' deactivated.");
            deactivateUserId.clear();
            handleViewAllStaff();
        } else {
            AlertHelper.showError("Error", "Failed to deactivate. Check the user ID.");
        }
    }

    @FXML
    private void handleReactivateStaff() {
        String userId = reactivateUserId.getText().trim();
        if (userId.isEmpty()) {
            AlertHelper.showError("Validation Error", "User ID is required.");
            return;
        }
        boolean ok = authService.changeUserStatus(userId, "ACTIVE");
        if (ok) {
            AlertHelper.showInfo("Success", "Account '" + userId + "' reactivated.");
            reactivateUserId.clear();
            handleViewAllStaff();
        } else {
            AlertHelper.showError("Error", "Failed to reactivate. Check the user ID.");
        }
    }

    @FXML
    private void hideStaffForms() {
        setVisible(addStaffForm,    false);
        setVisible(deactivateForm,  false);
        setVisible(reactivateForm,  false);
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        session.clear();
        SceneNavigator.navigateTo("login.fxml", "Supermarket Automation System – Login");
    }

    // ── Table Wiring ──────────────────────────────────────────────────────────
    private void setupTables() {
        // Inventory Status
        safe(invItemCodeCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        safe(invStockCol)      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStockLevel()).asObject());
        safe(invUpdatedByCol)  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUpdatedBy()));
        safe(invLastUpdatedCol).setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getLastUpdated()));

        // Low Stock
        safe(lsItemCodeCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        safe(lsItemNameCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        safe(lsReorderCol) .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getReorderLevel()).asObject());
        safe(lsCategoryCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));

        // Transactions
        safe(txnIdCol)    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId()));
        safe(txnDateCol)  .setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTransactionDate()));
        safe(txnAmountCol).setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotalAmount()).asObject());
        safe(txnStatusCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        safe(txnStaffCol) .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalesStaffId()));

        // Price History
        safe(phOldPriceCol) .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getOldPrice()).asObject());
        safe(phNewPriceCol) .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getNewPrice()).asObject());
        safe(phChangedAtCol).setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getChangedAt()));
        safe(phChangedByCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChangedBy()));

        // Staff
        safe(stUserIdCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserId()));
        safe(stRoleCol)     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        safe(stStatusCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        safe(stCreatedAtCol).setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCreatedAt()));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private void showPanel(VBox target) {
        VBox[] panels = {panelWelcome, panelAddItem, panelUpdatePrice,
                         panelInventoryStatus, panelLowStock,
                         panelTransactions, panelPriceHistory, panelStaffManagement};
        for (VBox p : panels) setVisible(p, false);
        setVisible(target, true);
    }

    private void setVisible(VBox box, boolean visible) {
        if (box != null) { box.setVisible(visible); box.setManaged(visible); }
    }

    /** Null-safe wrapper so missing @FXML injections don't crash table setup. */
    private <S, T> TableColumn<S, T> safe(TableColumn<S, T> col) {
        if (col == null) return new TableColumn<>();
        return col;
    }
}

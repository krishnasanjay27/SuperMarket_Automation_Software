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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.*;
import service.AuthService;
import service.InventoryService;
import service.ItemService;
import service.ReportService;
import service.SalesReportService;
import service.VendorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ManagerDashboardController {

    // ── Navigation Panels ─────────────────────────────────────────────────────
    @FXML private Label  managerIdLabel;
    @FXML private VBox   panelWelcome;
    @FXML private VBox   panelAddItem;
    @FXML private VBox   panelUpdatePrice;
    @FXML private VBox   panelUpdateItemVendor;
    @FXML private VBox   panelInventoryStatus;
    @FXML private VBox   panelLowStock;
    @FXML private VBox   panelTransactions;
    @FXML private VBox   panelPriceHistory;
    @FXML private VBox   panelStaffManagement;
    @FXML private VBox   panelRemoveItem;

    // ── Add Item Form ─────────────────────────────────────────────────────────
    @FXML private TextField aiItemCode;
    @FXML private TextField aiItemName;
    @FXML private TextField aiPrice;
    @FXML private TextField aiCostPrice;
    @FXML private TextField aiReorderLevel;
    @FXML private TextField aiCategory;
    @FXML private TextField aiInitialStock;
    @FXML private TextField aiReturnDuration;

    @FXML private ComboBox<Vendor> aiVendorCombo;

    // ── Update Price Form ─────────────────────────────────────────────────────
    @FXML private TextField upItemCode;
    @FXML private TextField upNewPrice;

    // ── Remove Item Form ──────────────────────────────────────────────────────
    @FXML private TextField removeItemCode;

    // ── Update Item Vendor Form ───────────────────────────────────────────────
    @FXML private TextField uivItemCode;
    @FXML private ComboBox<Vendor> uivVendorCombo;

    // ── Inventory Status Table ────────────────────────────────────────────────
    @FXML private TableView<model.InventoryVendorStatus>            inventoryTable;
    @FXML private TableColumn<model.InventoryVendorStatus, String>  invItemCodeCol;
    @FXML private TableColumn<model.InventoryVendorStatus, String>  invItemNameCol;
    @FXML private TableColumn<model.InventoryVendorStatus, Integer> invStockCol;
    @FXML private TableColumn<model.InventoryVendorStatus, String>  invVendorCol;
    @FXML private TableColumn<model.InventoryVendorStatus, Integer> invReturnDurationCol;
    @FXML private TableColumn<model.InventoryVendorStatus, String>  invUpdatedByCol;
    @FXML private TableColumn<model.InventoryVendorStatus, Object>  invLastUpdatedCol;

    // ── Low Stock Table ───────────────────────────────────────────────────────
    @FXML private TableView<LowStockVendorAlert>             lowStockTable;
    @FXML private TableColumn<LowStockVendorAlert, String>   lsItemNameCol;
    @FXML private TableColumn<LowStockVendorAlert, Integer>  lsStockCol;
    @FXML private TableColumn<LowStockVendorAlert, Integer>  lsReorderCol;
    @FXML private TableColumn<LowStockVendorAlert, String>   lsVendorNameCol;
    @FXML private TableColumn<LowStockVendorAlert, String>   lsVendorPhoneCol;

    // ── Transactions Table ────────────────────────────────────────────────────
    @FXML private TableView<SalesTransaction>            txnTable;
    @FXML private TableColumn<SalesTransaction, String>  txnIdCol;
    @FXML private TableColumn<SalesTransaction, Object>  txnDateCol;
    @FXML private TableColumn<SalesTransaction, Double>  txnAmountCol;
    @FXML private TableColumn<SalesTransaction, String>  txnStatusCol;
    @FXML private TableColumn<SalesTransaction, String>  txnStaffCol;
    @FXML private RadioButton txnRbToday;
    @FXML private RadioButton txnRbThisMonth;
    @FXML private RadioButton txnRbCustomRange;
    @FXML private HBox        txnDateRangeBox;
    @FXML private DatePicker  startDatePicker;
    @FXML private DatePicker  endDatePicker;

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

    // ── Vendor Management ─────────────────────────────────────────────────────
    @FXML private VBox panelVendorManagement;
    @FXML private TableView<Vendor>              vendorTable;
    @FXML private TableColumn<Vendor, Integer>   vIdCol;
    @FXML private TableColumn<Vendor, String>    vNameCol;
    @FXML private TableColumn<Vendor, String>    vPhoneCol;
    @FXML private TableColumn<Vendor, String>    vEmailCol;
    @FXML private TableColumn<Vendor, String>    vAddressCol;

    @FXML private VBox      addVendorForm;
    @FXML private TextField newVendorName;
    @FXML private TextField newVendorPhone;
    @FXML private TextField newVendorEmail;
    @FXML private TextField newVendorAddress;

    @FXML private VBox      updateVendorForm;
    @FXML private TextField updVendorId;
    @FXML private TextField updVendorPhone;
    @FXML private TextField updVendorEmail;
    @FXML private TextField updVendorAddress;

    @FXML private VBox      deleteVendorForm;
    @FXML private TextField deleteVendorId;

    // ── Sales Reports ─────────────────────────────────────────────────────────────
    @FXML private VBox panelProfitBySale;
    @FXML private VBox panelProfitByItem;

    @FXML private RadioButton saleRbToday;
    @FXML private RadioButton saleRbThisMonth;
    @FXML private RadioButton saleRbCustomRange;
    @FXML private DatePicker  saleDpStart;
    @FXML private DatePicker  saleDpEnd;

    @FXML private TableView<ProfitBySaleDTO>            saleReportTable;
    @FXML private TableColumn<ProfitBySaleDTO, String>  saleTxnIdCol;
    @FXML private TableColumn<ProfitBySaleDTO, Double>  saleRevCol;
    @FXML private TableColumn<ProfitBySaleDTO, Double>  saleCostCol;
    @FXML private TableColumn<ProfitBySaleDTO, Double>  saleProfitCol;
    @FXML private BarChart<String, Number>              saleBarChart;

    @FXML private RadioButton itemRbToday;
    @FXML private RadioButton itemRbThisMonth;
    @FXML private RadioButton itemRbCustomRange;
    @FXML private HBox        saleDateRangeBox;
    @FXML private HBox        itemDateRangeBox;
    @FXML private DatePicker  itemDpStart;
    @FXML private DatePicker  itemDpEnd;

    @FXML private TableView<ProfitByItemDTO>            itemReportTable;
    @FXML private TableColumn<ProfitByItemDTO, String>  itemCodeCol;
    @FXML private TableColumn<ProfitByItemDTO, String>  itemNameCol;
    @FXML private TableColumn<ProfitByItemDTO, Integer> itemTotalSoldCol;
    @FXML private TableColumn<ProfitByItemDTO, Double>  itemProfitCol;
    @FXML private BarChart<String, Number>              itemBarChart;

    private ToggleGroup saleToggleGroup;
    private ToggleGroup itemToggleGroup;
    private ToggleGroup txnToggleGroup;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ── Services ──────────────────────────────────────────────────────────────
    private final InventoryService   inventoryService   = new InventoryService();
    private final ReportService      reportService      = new ReportService();
    private final VendorService      vendorService      = new VendorService();
    private final ItemService        itemService        = new ItemService();
    private final AuthService        authService        = new AuthService();
    private final SalesReportService salesReportService = new SalesReportService();
    private final SessionManager     session            = SessionManager.getInstance();



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
        // Wire ToggleGroups for report panels
        saleToggleGroup = new ToggleGroup();
        if (saleRbToday != null) saleRbToday.setToggleGroup(saleToggleGroup);
        if (saleRbThisMonth != null) saleRbThisMonth.setToggleGroup(saleToggleGroup);
        if (saleRbCustomRange != null) saleRbCustomRange.setToggleGroup(saleToggleGroup);

        itemToggleGroup = new ToggleGroup();
        if (itemRbToday != null) itemRbToday.setToggleGroup(itemToggleGroup);
        if (itemRbThisMonth != null) itemRbThisMonth.setToggleGroup(itemToggleGroup);
        if (itemRbCustomRange != null) itemRbCustomRange.setToggleGroup(itemToggleGroup);

        txnToggleGroup = new ToggleGroup();
        if (txnRbToday != null) txnRbToday.setToggleGroup(txnToggleGroup);
        if (txnRbThisMonth != null) txnRbThisMonth.setToggleGroup(txnToggleGroup);
        if (txnRbCustomRange != null) txnRbCustomRange.setToggleGroup(txnToggleGroup);

        // Show date range pickers only when Custom Range is selected
        if (saleToggleGroup != null && saleDateRangeBox != null) {
            saleToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                boolean custom = (newT == saleRbCustomRange);
                saleDateRangeBox.setVisible(custom);
                saleDateRangeBox.setManaged(custom);
            });
        }
        if (itemToggleGroup != null && itemDateRangeBox != null) {
            itemToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                boolean custom = (newT == itemRbCustomRange);
                itemDateRangeBox.setVisible(custom);
                itemDateRangeBox.setManaged(custom);
            });
        }
        if (txnToggleGroup != null && txnDateRangeBox != null) {
            txnToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                boolean custom = (newT == txnRbCustomRange);
                txnDateRangeBox.setVisible(custom);
                txnDateRangeBox.setManaged(custom);
            });
        }

        showPanel(panelWelcome);
    }

    // ── Sidebar Navigation ────────────────────────────────────────────────────
    @FXML private void showAddItem() {
        showPanel(panelAddItem);
        List<Vendor> vendors = vendorService.getAllVendors();
        aiVendorCombo.setItems(FXCollections.observableArrayList(vendors));
        if (vendors.isEmpty()) {
            AlertHelper.showInfo("No Vendors", "No vendors available. Item will be created without vendor assignment.");
        }
    }
    @FXML private void showRemoveItem()      { showPanel(panelRemoveItem); removeItemCode.clear(); }
    @FXML private void showUpdatePrice()     { showPanel(panelUpdatePrice); }
    @FXML private void showUpdateItemVendor(){
        showPanel(panelUpdateItemVendor);
        List<Vendor> vendors = vendorService.getAllVendors();
        uivVendorCombo.setItems(FXCollections.observableArrayList(vendors));
        if (vendors.isEmpty()) {
            AlertHelper.showInfo("No Vendors", "No vendors available to assign.");
        }
    }
    @FXML private void showInventoryStatus() { showPanel(panelInventoryStatus); handleViewInventoryStatus(); }
    @FXML private void showLowStock()        { showPanel(panelLowStock); handleViewLowStockItems(); }
    @FXML private void showTransactions()    { showPanel(panelTransactions); }
    @FXML private void showPriceHistory()    { showPanel(panelPriceHistory); }
    @FXML private void showStaffManagement() { showPanel(panelStaffManagement); }
    @FXML private void showVendorManagement(){ showPanel(panelVendorManagement); handleViewAllVendors(); }
    @FXML private void showProfitBySale()    { showPanel(panelProfitBySale); }
    @FXML private void showProfitByItem()    { showPanel(panelProfitByItem); }

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
        String returnDurStr = aiReturnDuration != null ? aiReturnDuration.getText().trim() : "0";

        if (itemCode.isEmpty() || itemName.isEmpty() || priceStr.isEmpty()
                || costStr.isEmpty() || reorderStr.isEmpty()
                || category.isEmpty() || stockStr.isEmpty() || returnDurStr.isEmpty()) {
            AlertHelper.showError("Validation Error", "All fields are required.");
            return;
        }

        double price, costPrice;
        int reorderLevel, initialStock, returnDur;
        try {
            price        = Double.parseDouble(priceStr);
            costPrice    = Double.parseDouble(costStr);
            reorderLevel = Integer.parseInt(reorderStr);
            initialStock = Integer.parseInt(stockStr);
            returnDur    = Integer.parseInt(returnDurStr);
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error",
                    "Price/Cost must be decimals; Reorder Level/Stock/Return Duration must be integers.");
            return;
        }

        Item item = new Item(itemCode, itemName, price, costPrice,
                reorderLevel, category, LocalDateTime.now());
        item.setReturnDurationDays(returnDur);

        Vendor selectedVendor = aiVendorCombo.getSelectionModel().getSelectedItem();
        if (selectedVendor == null) {
            AlertHelper.showError("Validation Error", "Selecting a Vendor is mandatory for new items.");
            return;
        }
        item.setVendorId(selectedVendor.getVendorId());

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
        aiInitialStock.clear(); aiVendorCombo.getSelectionModel().clearSelection();
        if (aiReturnDuration != null) aiReturnDuration.clear();
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

    // ── Update Item Vendor ────────────────────────────────────────────────────
    @FXML
    private void handleUpdateItemVendor() {
        String itemCode = uivItemCode.getText().trim();
        Vendor selectedVendor = uivVendorCombo.getValue();

        if (itemCode.isEmpty() || selectedVendor == null) {
            AlertHelper.showError("Validation Error", "Item code and Vendor selection are required.");
            return;
        }

        Item item = inventoryService.getItemByCode(itemCode);
        if (item == null) {
            AlertHelper.showError("Error", "Item not found: " + itemCode);
            return;
        }

        item.setVendorId(selectedVendor.getVendorId());
        boolean ok = itemService.updateItemVendor(item);
        if (ok) {
            AlertHelper.showInfo("Success", "Vendor updated for item '" + itemCode + "'.");
            handleClearUpdateItemVendor();
        } else {
            AlertHelper.showError("Error", "Failed to update item vendor.");
        }
    }

    @FXML
    private void handleClearUpdateItemVendor() { 
        uivItemCode.clear(); 
        uivVendorCombo.getSelectionModel().clearSelection(); 
    }

    // ── Remove Item ───────────────────────────────────────────────────────────
    @FXML
    private void handleRemoveItem() {
        String itemCode = removeItemCode.getText().trim();
        if (itemCode.isEmpty()) {
            AlertHelper.showError("Validation Error", "Please provide an Item Code.");
            return;
        }
        
        boolean confirm = AlertHelper.showConfirm("Confirm Deletion", "Are you sure you want to permanently remove item " + itemCode + "?");
        if (!confirm) return;

        boolean success = inventoryService.removeItem(itemCode);
        if (success) {
            AlertHelper.showInfo("Success", "Item successfully removed from the catalogue.");
            removeItemCode.clear();
        } else {
            AlertHelper.showError("Remove Failed", "Could not remove item. It may have existing transaction records, or the code is wrong.");
        }
    }

    // ── Inventory Status ──────────────────────────────────────────────────────
    @FXML
    private void handleViewInventoryStatus() {
        List<model.InventoryVendorStatus> records = reportService.getInventoryStatusWithVendor();
        inventoryTable.setItems(FXCollections.observableArrayList(records));
        if (records.isEmpty())
            AlertHelper.showInfo("Inventory Status", "No inventory records found.");
    }

    // ── Low Stock ─────────────────────────────────────────────────────────────
    @FXML
    private void handleViewLowStockItems() {
        List<LowStockVendorAlert> alerts = reportService.getLowStockItemsWithVendor();
        lowStockTable.setItems(FXCollections.observableArrayList(alerts));
        if (alerts.isEmpty())
            AlertHelper.showInfo("Low Stock", "No items are below their reorder level.");
    }

    // ── Transactions by Date ──────────────────────────────────────────────────
    @FXML
    private void handleViewTransactionsByDate() {
        LocalDate[] range = resolveDateRange(txnRbToday, txnRbThisMonth, txnRbCustomRange,
                                             startDatePicker, endDatePicker, "txn");
        if (range == null) return;

        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end   = range[1].atTime(LocalTime.MAX);

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

    // ── Vendor Management CRUD ────────────────────────────────────────────────
    @FXML
    private void handleViewAllVendors() {
        List<Vendor> vendors = vendorService.getAllVendors();
        vendorTable.setItems(FXCollections.observableArrayList(vendors));
        hideVendorForms();
    }

    @FXML private void showAddVendor()    { hideVendorForms(); setVisible(addVendorForm, true); }
    @FXML private void showUpdateVendor() {
        Vendor selected = vendorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Vendor Selected",
                    "Please select a vendor from the table above before clicking Update Vendor.");
            return;
        }
        hideVendorForms();
        setVisible(updateVendorForm, true);
        // Pre-fill with existing data so the user edits only what they need
        updVendorId.setText(String.valueOf(selected.getVendorId()));
        updVendorPhone.setText(selected.getPhone()   != null ? selected.getPhone()   : "");
        updVendorEmail.setText(selected.getEmail()   != null ? selected.getEmail()   : "");
        updVendorAddress.setText(selected.getAddress() != null ? selected.getAddress() : "");
        // Vendor ID is read-only — the user should not change it
        updVendorId.setEditable(false);
        updVendorId.setStyle("-fx-opacity: 0.6;");
    }
    @FXML private void showDeleteVendor() { hideVendorForms(); setVisible(deleteVendorForm, true); }

    @FXML
    private void handleAddVendor() {
        String name    = newVendorName.getText().trim();
        String phone   = newVendorPhone.getText().trim();
        String email   = newVendorEmail.getText().trim();
        String address = newVendorAddress.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            AlertHelper.showError("Validation Error", "Vendor Name and Phone are required.");
            return;
        }

        if (!phone.matches("[6-9]\\d{9}")) {
            AlertHelper.showError("Invalid Phone Number",
                    "Vendor phone must be a valid Indian mobile number:\n"
                    + "  \u2022 Exactly 10 digits\n"
                    + "  \u2022 Must start with 6, 7, 8, or 9\n"
                    + "Entered: \"" + phone + "\"");
            return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            AlertHelper.showError("Invalid Email",
                    "Please enter a valid email address (e.g. vendor@company.com)\n"
                    + "or leave it blank.");
            return;
        }

        boolean ok = vendorService.addVendor(name, phone, email, address);
        if (ok) {
            AlertHelper.showInfo("Success", "Vendor '" + name + "' added successfully.");
            newVendorName.clear(); newVendorPhone.clear();
            newVendorEmail.clear(); newVendorAddress.clear();
            handleViewAllVendors();
        } else {
            AlertHelper.showError("Error", "Failed to add vendor. Phone number may already be registered.");
        }
    }

    @FXML
    private void handleUpdateVendor() {
        String idStr   = updVendorId.getText().trim();
        String phone   = updVendorPhone.getText().trim();
        String email   = updVendorEmail.getText().trim();
        String address = updVendorAddress.getText().trim();

        if (idStr.isEmpty() || phone.isEmpty()) {
            AlertHelper.showError("Validation Error", "Vendor ID and Phone are required.");
            return;
        }

        int vid;
        try { vid = Integer.parseInt(idStr); }
        catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Vendor ID must be a valid integer.");
            return;
        }

        boolean ok = vendorService.updateVendor(vid, phone, email, address);
        if (ok) {
            AlertHelper.showInfo("Success", "Vendor ID " + vid + " updated successfully.");
            updVendorId.clear(); updVendorPhone.clear();
            updVendorEmail.clear(); updVendorAddress.clear();
            handleViewAllVendors();
        } else {
            AlertHelper.showError("Error", "Failed to update vendor. Ensure ID exists and phone is valid.");
        }
    }

    @FXML
    private void handleDeleteVendor() {
        String idStr = deleteVendorId.getText().trim();
        if (idStr.isEmpty()) {
            AlertHelper.showError("Validation Error", "Vendor ID is required.");
            return;
        }
        int vid;
        try { vid = Integer.parseInt(idStr); }
        catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Vendor ID must be a valid integer.");
            return;
        }

        boolean ok = vendorService.deleteVendor(vid);
        if (ok) {
            AlertHelper.showInfo("Success", "Vendor ID " + vid + " deleted.");
            deleteVendorId.clear();
            handleViewAllVendors();
        } else {
            AlertHelper.showError("Error", "Failed to delete. Vendor may be linked to active items.");
        }
    }

    @FXML
    private void hideVendorForms() {
        setVisible(addVendorForm, false);
        setVisible(updateVendorForm, false);
        setVisible(deleteVendorForm, false);
        // Reset the Vendor ID field so it's not permanently locked
        if (updVendorId != null) {
            updVendorId.setEditable(true);
            updVendorId.setStyle("");
        }
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
        safe(invItemNameCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        safe(invStockCol)      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStockLevel()).asObject());
        safe(invVendorCol)     .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVendorName() != null ? d.getValue().getVendorName() : "N/A"));
        safe(invReturnDurationCol).setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getReturnDurationDays()).asObject());
        safe(invUpdatedByCol)  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUpdatedBy()));
        safe(invLastUpdatedCol).setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getLastUpdated()));

        // Low Stock Vendor Alert
        safe(lsItemNameCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        safe(lsStockCol)      .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStockLevel()).asObject());
        safe(lsReorderCol)    .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getReorderLevel()).asObject());
        safe(lsVendorNameCol) .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVendorName() != null ? d.getValue().getVendorName() : "N/A"));
        safe(lsVendorPhoneCol).setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPhone() != null ? d.getValue().getPhone() : "N/A"));

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

        // Vendor
        safe(vIdCol)     .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getVendorId()).asObject());
        safe(vNameCol)   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVendorName()));
        safe(vPhoneCol)  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
        safe(vEmailCol)  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        safe(vAddressCol).setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAddress()));

        // When the user clicks a different vendor row while the update form is open, refresh the fields
        if (vendorTable != null) {
            vendorTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && updateVendorForm != null && updateVendorForm.isVisible()) {
                    updVendorId.setText(String.valueOf(newV.getVendorId()));
                    updVendorPhone.setText(newV.getPhone()   != null ? newV.getPhone()   : "");
                    updVendorEmail.setText(newV.getEmail()   != null ? newV.getEmail()   : "");
                    updVendorAddress.setText(newV.getAddress() != null ? newV.getAddress() : "");
                    updVendorId.setEditable(false);
                    updVendorId.setStyle("-fx-opacity: 0.6;");
                }
            });
        }

        // Sales Report Tables
        safe(saleTxnIdCol) .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId()));
        safe(saleRevCol)   .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getRevenue()).asObject());
        safe(saleCostCol)  .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getCost()).asObject());
        safe(saleProfitCol).setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getProfit()).asObject());

        safe(itemCodeCol)     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemCode()));
        safe(itemNameCol)     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        safe(itemTotalSoldCol).setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getTotalSold()).asObject());
        safe(itemProfitCol)   .setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getProfit()).asObject());
    }

    // ── Sales Reports ─────────────────────────────────────────────────────────────
    @FXML
    private void handleGenerateSaleReport() {
        LocalDate[] range = resolveDateRange(saleRbToday, saleRbThisMonth, saleRbCustomRange,
                                             saleDpStart, saleDpEnd, "sale");
        if (range == null) return;

        LocalDate start = range[0];
        LocalDate end   = range[1];
        String title    = buildChartTitle("Profit by Sale", saleRbToday, saleRbThisMonth, start, end);

        List<ProfitBySaleDTO> data = salesReportService.loadProfitBySaleReport(start, end, 20);
        saleReportTable.setItems(FXCollections.observableArrayList(data));
        saleBarChart.getData().clear();
        saleBarChart.setTitle(title);

        if (data.isEmpty()) {
            AlertHelper.showInfo("No Data", "No sales data found for selected period.");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Profit");
        for (ProfitBySaleDTO row : data) {
            series.getData().add(new XYChart.Data<>(row.getTransactionId(), row.getProfit()));
        }
        saleBarChart.getData().add(series);
    }

    @FXML
    private void handleGenerateItemReport() {
        LocalDate[] range = resolveDateRange(itemRbToday, itemRbThisMonth, itemRbCustomRange,
                                             itemDpStart, itemDpEnd, "item");
        if (range == null) return;

        LocalDate start = range[0];
        LocalDate end   = range[1];
        String title    = buildChartTitle("Profit by Item", itemRbToday, itemRbThisMonth, start, end);

        List<ProfitByItemDTO> data = salesReportService.loadProfitByItemReport(start, end, 20);
        itemReportTable.setItems(FXCollections.observableArrayList(data));
        itemBarChart.getData().clear();
        itemBarChart.setTitle(title);

        if (data.isEmpty()) {
            AlertHelper.showInfo("No Data", "No sales data found for selected period.");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Profit");
        for (ProfitByItemDTO row : data) {
            series.getData().add(new XYChart.Data<>(row.getItemName(), row.getProfit()));
        }
        itemBarChart.getData().add(series);
    }

    private LocalDate[] resolveDateRange(RadioButton rbToday, RadioButton rbThisMonth,
                                         RadioButton rbCustom, DatePicker dpStart,
                                         DatePicker dpEnd, String context) {
        if (rbToday != null && rbToday.isSelected()) {
            LocalDate today = LocalDate.now();
            return new LocalDate[]{today, today};
        }
        if (rbThisMonth != null && rbThisMonth.isSelected()) {
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            return new LocalDate[]{start, LocalDate.now()};
        }
        // Custom Range
        LocalDate start = dpStart != null ? dpStart.getValue() : null;
        LocalDate end   = dpEnd   != null ? dpEnd.getValue()   : null;
        if (start == null || end == null || start.isAfter(end)) {
            AlertHelper.showError("Invalid Date Range",
                "Invalid date range selected. Ensure both dates are set and start ≤ end.");
            return null;
        }
        return new LocalDate[]{start, end};
    }

    private String buildChartTitle(String base, RadioButton rbToday, RadioButton rbThisMonth,
                                   LocalDate start, LocalDate end) {
        if (rbToday != null && rbToday.isSelected())     return base + " (Today)";
        if (rbThisMonth != null && rbThisMonth.isSelected()) return base + " (This Month)";
        return base + " (" + start.format(DMY) + " to " + end.format(DMY) + ")";
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private void showPanel(VBox target) {
        VBox[] panels = {panelWelcome, panelAddItem, panelUpdatePrice, panelUpdateItemVendor, panelRemoveItem,
                         panelInventoryStatus, panelLowStock,
                         panelTransactions, panelPriceHistory,
                         panelStaffManagement, panelVendorManagement,
                         panelProfitBySale, panelProfitByItem};
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

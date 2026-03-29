package app.controllers;

import app.SceneNavigator;
import app.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.InventoryService;

public class InventoryDashboardController {

    @FXML private Label    staffIdLabel;

    @FXML private VBox     welcomePanel;
    @FXML private VBox     updateStockPanel;
    @FXML private VBox     setStockPanel;
    @FXML private VBox     checkStockPanel;

    @FXML private TextField usItemCodeField;
    @FXML private TextField usQtyChangeField;
    @FXML private Label     usResultLabel;

    @FXML private TextField ssItemCodeField;
    @FXML private TextField ssNewStockField;
    @FXML private Label     ssResultLabel;

    @FXML private TextField csItemCodeField;
    @FXML private Label     csResultLabel;

    private final InventoryService inventoryService = new InventoryService();
    private final SessionManager   session          = SessionManager.getInstance();

    @FXML
    public void initialize() {
        if (staffIdLabel != null) {
            staffIdLabel.setText("Logged in as: " + session.getUserId());
        }
        showPanel(welcomePanel);
    }

    @FXML
    private void handleUpdateStock() {
        showPanel(updateStockPanel);
        clearLabel(usResultLabel);
    }

    @FXML
    private void handleSetStockLevel() {
        showPanel(setStockPanel);
        clearLabel(ssResultLabel);
    }

    @FXML
    private void handleCheckStockLevel() {
        showPanel(checkStockPanel);
        clearLabel(csResultLabel);
    }

    @FXML
    private void handleUpdateStockSubmit() {
        String itemCode = usItemCodeField.getText().trim();
        String qtyStr   = usQtyChangeField.getText().trim();

        if (itemCode.isEmpty() || qtyStr.isEmpty()) {
            usResultLabel.setText("❌ All fields are required.");
            usResultLabel.setStyle("-fx-text-fill: #C62828;");
            return;
        }
        int qty;
        try { qty = Integer.parseInt(qtyStr); }
        catch (NumberFormatException e) {
            usResultLabel.setText("❌ Quantity must be a whole number.");
            usResultLabel.setStyle("-fx-text-fill: #C62828;");
            return;
        }

        boolean ok = inventoryService.updateStock(itemCode, qty, session.getUserId());
        if (ok) {
            usResultLabel.setText("✔ Stock updated for item: " + itemCode);
            usResultLabel.setStyle("-fx-text-fill: #1B7A3E;");
        } else {
            usResultLabel.setText("❌ Update failed. Check item code and quantity.");
            usResultLabel.setStyle("-fx-text-fill: #C62828;");
        }
    }

    @FXML
    private void handleSetStockSubmit() {
        String itemCode    = ssItemCodeField.getText().trim();
        String newStockStr = ssNewStockField.getText().trim();

        if (itemCode.isEmpty() || newStockStr.isEmpty()) {
            ssResultLabel.setText("❌ All fields are required.");
            ssResultLabel.setStyle("-fx-text-fill: #C62828;");
            return;
        }
        int newStock;
        try { newStock = Integer.parseInt(newStockStr); }
        catch (NumberFormatException e) {
            ssResultLabel.setText("❌ Stock level must be a whole number.");
            ssResultLabel.setStyle("-fx-text-fill: #C62828;");
            return;
        }

        boolean ok = inventoryService.setStockLevel(itemCode, newStock, session.getUserId());
        if (ok) {
            ssResultLabel.setText("✔ Stock level set to " + newStock + " for item: " + itemCode);
            ssResultLabel.setStyle("-fx-text-fill: #1B7A3E;");
        } else {
            ssResultLabel.setText("❌ Failed to set stock level. Check item code.");
            ssResultLabel.setStyle("-fx-text-fill: #C62828;");
        }
    }

    @FXML
    private void handleCheckStockSubmit() {
        String itemCode = csItemCodeField.getText().trim();
        if (itemCode.isEmpty()) {
            csResultLabel.setText("❌ Item code is required.");
            csResultLabel.setStyle("-fx-text-fill: #C62828;");
            return;
        }
        int level = inventoryService.getStockLevel(itemCode);
        if (level < 0) {
            csResultLabel.setText("❌ No inventory record found for: " + itemCode);
            csResultLabel.setStyle("-fx-text-fill: #C62828;");
        } else {
            csResultLabel.setText("✔ Item: " + itemCode + "   |   Current Stock: " + level + " units");
            csResultLabel.setStyle("-fx-text-fill: #1B7A3E;");
        }
    }

    @FXML
    private void handleClear() {
        if (updateStockPanel.isVisible()) {
            usItemCodeField.clear(); usQtyChangeField.clear(); clearLabel(usResultLabel);
        } else if (setStockPanel.isVisible()) {
            ssItemCodeField.clear(); ssNewStockField.clear(); clearLabel(ssResultLabel);
        } else if (checkStockPanel.isVisible()) {
            csItemCodeField.clear(); clearLabel(csResultLabel);
        }
    }

    @FXML
    private void handleLogout() {
        session.clear();
        SceneNavigator.navigateTo("login.fxml", "Supermarket Automation System – Login");
    }

    private void showPanel(VBox target) {
        for (VBox p : new VBox[]{welcomePanel, updateStockPanel, setStockPanel, checkStockPanel}) {
            if (p != null) { p.setVisible(false); p.setManaged(false); }
        }
        if (target != null) { target.setVisible(true); target.setManaged(true); }
    }

    private void clearLabel(Label lbl) {
        if (lbl != null) { lbl.setText(""); }
    }
}

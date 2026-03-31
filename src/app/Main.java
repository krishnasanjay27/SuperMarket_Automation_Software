package app;

import model.InventoryRecord;
import model.Item;
import model.PriceHistory;
import model.SalesTransaction;
import model.TransactionItem;
import model.UserAccount;
import service.AuthService;
import service.BillService;
import service.InventoryService;
import service.ReportService;
import service.TransactionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Main – End-to-end CLI test harness for the Supermarket Automation System.
 *
 * This class systematically verifies every backend layer:
 *   Authentication → Inventory → Transactions → Reports → Triggers
 *
 * Each operation prints its name, inputs, and result so behaviour
 * can be confirmed before GUI development begins.
 */
public class Main {

    // ----------------------------------------------------------------
    // Shared state
    // ----------------------------------------------------------------
    private static final Scanner          sc          = new Scanner(System.in);
    private static final AuthService      authService = new AuthService();
    private static final BillService      billService = new BillService();
    private static final InventoryService invService  = new InventoryService();
    private static final TransactionService txnService = new TransactionService();
    private static final ReportService    rptService  = new ReportService();

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ================================================================
    // ENTRY POINT
    // ================================================================
    public static void main(String[] args) {
        printBanner();
        while (true) {
            UserAccount user = login();
            if (user == null) {
                System.out.println("\nExiting system. Goodbye.");
                break;
            }
            switch (user.getRole()) {
                case "Manager"        -> managerMenu(user);
                case "SalesStaff"     -> salesStaffMenu(user);
                case "InventoryStaff" -> inventoryStaffMenu(user);
                default -> printFail("Unknown role: " + user.getRole());
            }
        }
        sc.close();
    }

    // ================================================================
    // STEP 1 – LOGIN
    // ================================================================

    /**
     * Prompts for credentials with retry. Returns null only if user types "exit".
     */
    private static UserAccount login() {
        printSection("STEP 1 — LOGIN VERIFICATION");
        System.out.println("Type 'exit' at any prompt to quit.\n");

        while (true) {
            System.out.print("  userId   : ");
            String userId = sc.nextLine().trim();
            if ("exit".equalsIgnoreCase(userId)) return null;

            System.out.print("  password : ");
            String password = sc.nextLine().trim();
            if ("exit".equalsIgnoreCase(password)) return null;

            printOp("AuthService.login()", "userId=" + userId);
            UserAccount user = authService.login(userId, password);

            if (user != null) {
                printPass("Login successful. Role: " + user.getRole());
                return user;
            }
            printFail("Login failed – invalid credentials or account inactive. Retry.\n");
        }
    }

    // ================================================================
    // STEP 2 – MANAGER TEST MENU
    // ================================================================
    private static void managerMenu(UserAccount user) {
        while (true) {
            printSection("MANAGER TEST MENU  (logged in as: " + user.getUserId() + ")");
            System.out.println("  -- Inventory & Reports --");
            System.out.println("  1.  Add new item");
            System.out.println("  2.  Update item price");
            System.out.println("  3.  View low-stock items");
            System.out.println("  4.  View inventory status");
            System.out.println("  5.  View transactions by date range");
            System.out.println("  6.  View price history for item");
            System.out.println("  -- Staff Management --");
            System.out.println("  8.  View all staff");
            System.out.println("  9.  Add new staff member");
            System.out.println("  10. Deactivate staff account");
            System.out.println("  11. Reactivate staff account");
            System.out.println("  -- Session --");
            System.out.println("  7.  Logout");
            System.out.print("Choice: ");

            switch (readInt()) {

                // ----------------------------------------------------------
                case 1 -> {
                    printOp("InventoryService.addNewItem()", "");
                    System.out.print("  itemCode     : "); String code     = sc.nextLine().trim();
                    System.out.print("  itemName     : "); String name     = sc.nextLine().trim();
                    System.out.print("  price        : "); double price    = readDouble();
                    System.out.print("  costPrice    : "); double cost     = readDouble();
                    System.out.print("  reorderLevel : "); int reorder     = readInt();
                    System.out.print("  category     : "); String cat      = sc.nextLine().trim();
                    System.out.print("  initialStock : "); int stock       = readInt();

                    System.out.println("  → Inputs: itemCode=" + code + ", name=" + name
                        + ", price=" + price + ", cost=" + cost
                        + ", reorder=" + reorder + ", cat=" + cat
                        + ", stock=" + stock + ", updatedBy=" + user.getUserId());

                    Item item = new Item();
                    item.setItemCode(code);
                    item.setItemName(name);
                    item.setPrice(price);
                    item.setCostPrice(cost);
                    item.setReorderLevel(reorder);
                    item.setCategory(cat.isBlank() ? "General" : cat);
                    item.setCreatedAt(LocalDateTime.now());

                    boolean ok = invService.addNewItem(item, stock, user.getUserId());
                    if (ok) printPass("Item '" + code + "' added with initial stock " + stock + ".");
                    else    printFail("addNewItem() failed.");
                }

                // ----------------------------------------------------------
                case 2 -> {
                    printOp("InventoryService.updateItemPrice()", "");
                    System.out.print("  itemCode : "); String code  = sc.nextLine().trim();
                    System.out.print("  newPrice : "); double price = readDouble();

                    System.out.println("  → Inputs: itemCode=" + code + ", newPrice=" + price
                        + ", updatedBy=" + user.getUserId());

                    boolean ok = invService.updateItemPrice(code, price, user.getUserId());
                    if (ok) {
                        printPass("Price updated for '" + code + "' → " + price);
                        // Verify PriceHistory was recorded
                        List<PriceHistory> hist = rptService.getPriceHistory(code);
                        if (!hist.isEmpty()) {
                            PriceHistory latest = hist.get(0);
                            printPass("PriceHistory entry confirmed: "
                                + latest.getOldPrice() + " → " + latest.getNewPrice()
                                + " by " + latest.getChangedBy()
                                + " at " + (latest.getChangedAt() != null
                                    ? latest.getChangedAt().format(DT_FMT) : "N/A"));
                        } else {
                            printFail("WARNING: PriceHistory entry NOT found after update.");
                        }
                    } else {
                        printFail("updateItemPrice() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 3 -> {
                    printOp("ReportService.getLowStockItemsWithVendor()", "no inputs");
                    List<model.LowStockVendorAlert> low = rptService.getLowStockItemsWithVendor();
                    if (low.isEmpty()) {
                        printPass("All items are sufficiently stocked.");
                    } else {
                        printPass("Low-stock items found: " + low.size());
                        System.out.printf("  %-25s %-10s %-10s %-20s %-15s%n",
                            "ItemName", "Stock", "Reorder", "Vendor", "Phone");
                        System.out.println("  " + "-".repeat(85));
                        for (model.LowStockVendorAlert alert : low) {
                            System.out.printf("  %-25s %-10d %-10d %-20s %-15s%n",
                                alert.getItemName(), alert.getStockLevel(), alert.getReorderLevel(),
                                alert.getVendorName() != null ? alert.getVendorName() : "N/A",
                                alert.getPhone() != null ? alert.getPhone() : "N/A");
                        }
                    }
                }

                // ----------------------------------------------------------
                case 4 -> {
                    printOp("ReportService.getInventoryStatus()", "no inputs");
                    List<InventoryRecord> records = rptService.getInventoryStatus();
                    if (records.isEmpty()) {
                        printFail("No inventory records found.");
                    } else {
                        printPass("Inventory snapshot – " + records.size() + " record(s):");
                        System.out.printf("  %-12s %-10s %-22s %-15s%n",
                            "ItemCode", "Stock", "LastUpdated", "UpdatedBy");
                        System.out.println("  " + "-".repeat(62));
                        for (InventoryRecord r : records) {
                            System.out.printf("  %-12s %-10d %-22s %-15s%n",
                                r.getItemCode(),
                                r.getStockLevel(),
                                r.getLastUpdated() != null ? r.getLastUpdated().format(DT_FMT) : "N/A",
                                r.getUpdatedBy() != null   ? r.getUpdatedBy()                 : "—");
                        }
                    }
                }

                // ----------------------------------------------------------
                case 5 -> {
                    printOp("ReportService.getTransactionsByDateRange()", "");
                    LocalDateTime start = readDateTime("  startDateTime (yyyy-MM-dd HH:mm) : ");
                    LocalDateTime end   = readDateTime("  endDateTime   (yyyy-MM-dd HH:mm) : ");
                    if (start == null || end == null) {
                        printFail("Invalid date format. Use yyyy-MM-dd HH:mm");
                        break;
                    }
                    System.out.println("  → Inputs: start=" + start.format(DT_FMT)
                        + ", end=" + end.format(DT_FMT));

                    List<SalesTransaction> txns = rptService.getTransactionsByDateRange(start, end);
                    if (txns.isEmpty()) {
                        printPass("No transactions found in date range.");
                    } else {
                        printPass("Transactions found: " + txns.size());
                        System.out.printf("  %-20s %-22s %-10s %-12s %-12s%n",
                            "TransactionId", "Date", "Total", "Status", "StaffId");
                        System.out.println("  " + "-".repeat(78));
                        for (SalesTransaction t : txns) {
                            System.out.printf("  %-20s %-22s %-10.2f %-12s %-12s%n",
                                t.getTransactionId(),
                                t.getTransactionDate() != null
                                    ? t.getTransactionDate().format(DT_FMT) : "N/A",
                                t.getTotalAmount(),
                                t.getStatus(),
                                t.getSalesStaffId());
                        }
                    }
                }

                // ----------------------------------------------------------
                case 6 -> {
                    printOp("ReportService.getPriceHistory()", "");
                    System.out.print("  itemCode : ");
                    String code = sc.nextLine().trim();
                    System.out.println("  → Inputs: itemCode=" + code);

                    List<PriceHistory> hist = rptService.getPriceHistory(code);
                    if (hist.isEmpty()) {
                        printPass("No price history found for: " + code);
                    } else {
                        printPass("Price history for '" + code + "' — " + hist.size() + " record(s):");
                        System.out.printf("  %-6s %-12s %-12s %-22s %-12s%n",
                            "ID", "OldPrice", "NewPrice", "ChangedAt", "ChangedBy");
                        System.out.println("  " + "-".repeat(66));
                        for (PriceHistory ph : hist) {
                            System.out.printf("  %-6d %-12.2f %-12.2f %-22s %-12s%n",
                                ph.getPriceHistoryId(),
                                ph.getOldPrice(),
                                ph.getNewPrice(),
                                ph.getChangedAt() != null ? ph.getChangedAt().format(DT_FMT) : "N/A",
                                ph.getChangedBy());
                        }
                    }
                }

                // ----------------------------------------------------------
                case 7 -> { printPass("Manager logged out."); return; }

                // ----------------------------------------------------------
                // STAFF MANAGEMENT
                // ----------------------------------------------------------
                case 8 -> {
                    printOp("UserAccountDAO.getAllUsers()", "no inputs");
                    List<UserAccount> staff = new dao.UserAccountDAO().getAllUsers();
                    if (staff.isEmpty()) {
                        printPass("No user accounts found.");
                    } else {
                        printPass("Staff accounts: " + staff.size());
                        System.out.printf("  %-15s %-15s %-12s %s%n",
                            "UserId", "Role", "Status", "CreatedAt");
                        System.out.println("  " + "-".repeat(60));
                        for (UserAccount u : staff) {
                            System.out.printf("  %-15s %-15s %-12s %s%n",
                                u.getUserId(), u.getRole(), u.getStatus(),
                                u.getCreatedAt() != null
                                    ? u.getCreatedAt().format(DT_FMT) : "N/A");
                        }
                    }
                }

                case 9 -> {
                    printOp("AuthService.createUser()", "");
                    System.out.print("  userId   : "); String newId   = sc.nextLine().trim();
                    System.out.print("  password : "); String newPass = sc.nextLine().trim();
                    System.out.print("  role (SalesStaff / InventoryStaff / Manager) : ");
                    String newRole = sc.nextLine().trim();
                    System.out.println("  -> Inputs: userId=" + newId + ", role=" + newRole);

                    UserAccount newUser = new UserAccount();
                    newUser.setUserId(newId);
                    newUser.setPassword(newPass);
                    newUser.setRole(newRole);

                    boolean created = authService.createUser(newUser);
                    if (created) printPass("Staff account '" + newId + "' created with role '" + newRole + "'.");
                    else         printFail("createUser() failed. UserId may already exist.");
                }

                case 10 -> {
                    printOp("AuthService.changeUserStatus(INACTIVE)", "");
                    System.out.print("  userId to deactivate : ");
                    String deactId = sc.nextLine().trim();
                    System.out.println("  -> Inputs: userId=" + deactId + ", status=INACTIVE");
                    boolean ok = authService.changeUserStatus(deactId, "INACTIVE");
                    if (ok) printPass("User '" + deactId + "' deactivated. Login will be rejected.");
                    else    printFail("changeUserStatus(INACTIVE) failed.");
                }

                case 11 -> {
                    printOp("AuthService.changeUserStatus(ACTIVE)", "");
                    System.out.print("  userId to reactivate : ");
                    String reactId = sc.nextLine().trim();
                    System.out.println("  -> Inputs: userId=" + reactId + ", status=ACTIVE");
                    boolean ok = authService.changeUserStatus(reactId, "ACTIVE");
                    if (ok) printPass("User '" + reactId + "' reactivated. Login allowed.");
                    else    printFail("changeUserStatus(ACTIVE) failed.");
                }

                default -> printFail("Invalid choice. Enter 1-11.");
            }
        }
    }

    // ================================================================
    // STEP 3 – INVENTORY STAFF TEST MENU
    // ================================================================
    private static void inventoryStaffMenu(UserAccount user) {
        while (true) {
            printSection("INVENTORY STAFF TEST MENU  (logged in as: " + user.getUserId() + ")");
            System.out.println("  1. Update stock (add/remove units)");
            System.out.println("  2. Set stock level (physical count)");
            System.out.println("  3. Check stock level");
            System.out.println("  4. Logout");
            System.out.print("Choice: ");

            switch (readInt()) {

                case 1 -> {
                    printOp("InventoryService.updateStock()", "");
                    System.out.print("  itemCode       : "); String code  = sc.nextLine().trim();
                    System.out.print("  quantityChange : "); int    delta = readInt();
                    System.out.println("  → Inputs: itemCode=" + code + ", quantityChange=" + delta
                        + ", updatedBy=" + user.getUserId());

                    int before = invService.getStockLevel(code);
                    boolean ok = invService.updateStock(code, delta, user.getUserId());
                    int after  = invService.getStockLevel(code);

                    if (ok) printPass("Stock updated successfully. Before=" + before + " → After=" + after);
                    else    printFail("updateStock() failed. Stock remains: " + before);
                }

                case 2 -> {
                    printOp("InventoryService.setStockLevel()", "");
                    System.out.print("  itemCode  : "); String code  = sc.nextLine().trim();
                    System.out.print("  newStock  : "); int newStock = readInt();
                    System.out.println("  → Inputs: itemCode=" + code + ", newStock=" + newStock
                        + ", updatedBy=" + user.getUserId());

                    boolean ok   = invService.setStockLevel(code, newStock, user.getUserId());
                    int verified = invService.getStockLevel(code);

                    if (ok) printPass("Stock level set. Verified stock: " + verified);
                    else    printFail("setStockLevel() failed.");
                }

                case 3 -> {
                    printOp("InventoryService.getStockLevel()", "");
                    System.out.print("  itemCode : "); String code = sc.nextLine().trim();
                    System.out.println("  → Inputs: itemCode=" + code);

                    int level = invService.getStockLevel(code);
                    if (level < 0) printFail("No inventory record for: " + code);
                    else           printPass("Stock level for '" + code + "': " + level + " units.");
                }

                case 4 -> { printPass("Inventory staff logged out."); return; }
                default -> printFail("Invalid choice. Enter 1–4.");
            }
        }
    }

    // ================================================================
    // STEP 4 – SALES STAFF TEST MENU
    // ================================================================
    private static void salesStaffMenu(UserAccount user) {
        String activeTxnId = null;     // track in-progress transaction ID across menu selections

        while (true) {
            printSection("SALES STAFF TEST MENU  (logged in as: " + user.getUserId() + ")");
            if (activeTxnId != null)
                System.out.println("  [Active Transaction: " + activeTxnId + "]");
            System.out.println("  1. Create transaction");
            System.out.println("  2. Add item to transaction");
            System.out.println("  3. Update item quantity");
            System.out.println("  4. Remove item from transaction");
            System.out.println("  5. Finalize transaction");
            System.out.println("  6. Abort transaction");
            System.out.println("  7. Logout");
            System.out.println("  8. Print bill / receipt");
            System.out.print("Choice: ");

            switch (readInt()) {

                // ----------------------------------------------------------
                case 1 -> {
                    printOp("TransactionService.createTransaction()", "");
                    System.out.print("  salesStaffId : ");
                    String staffId = sc.nextLine().trim();
                    System.out.println("  → Inputs: salesStaffId=" + staffId);

                    String txnId = txnService.createTransaction(staffId);
                    if (txnId != null) {
                        activeTxnId = txnId;
                        printPass("Transaction created successfully: " + txnId);
                    } else {
                        printFail("createTransaction() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 2 -> {
                    printOp("TransactionService.addItemToTransaction()", "");
                    System.out.print("  transactionId : ");
                    String txnId = sc.nextLine().trim();
                    if (txnId.isBlank() && activeTxnId != null) {
                        txnId = activeTxnId;
                        System.out.println("  (using active transaction: " + activeTxnId + ")");
                    }
                    System.out.print("  itemCode      : "); String itemCode = sc.nextLine().trim();
                    System.out.print("  quantity      : "); int    qty      = readInt();

                    System.out.println("  → Inputs: transactionId=" + txnId
                        + ", itemCode=" + itemCode + ", quantity=" + qty);

                    boolean ok = txnService.addItemToTransaction(txnId, itemCode, qty);
                    if (ok) {
                        printPass("Item '" + itemCode + "' x" + qty + " added to transaction " + txnId);
                        // Show updated transaction total
                        verifyTransactionTotal(txnId);
                    } else {
                        printFail("addItemToTransaction() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 3 -> {
                    printOp("TransactionService.updateItemQuantity()", "");
                    System.out.print("  transactionItemId : "); int  itemId  = readInt();
                    System.out.print("  newQuantity       : "); int  newQty  = readInt();
                    System.out.println("  → Inputs: transactionItemId=" + itemId + ", newQuantity=" + newQty);

                    boolean ok = txnService.updateItemQuantity(itemId, newQty);
                    if (ok) printPass("Quantity updated. lineTotal recalculated by DAO.");
                    else    printFail("updateItemQuantity() failed.");
                }

                // ----------------------------------------------------------
                case 4 -> {
                    printOp("TransactionService.removeItemFromTransaction()", "");
                    System.out.print("  transactionItemId : "); int removeId = readInt();
                    System.out.println("  → Inputs: transactionItemId=" + removeId);

                    boolean ok = txnService.removeItemFromTransaction(removeId);
                    if (ok) {
                        printPass("Item removed. Transaction total updated automatically by DB trigger.");
                        if (activeTxnId != null) verifyTransactionTotal(activeTxnId);
                    } else {
                        printFail("removeItemFromTransaction() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 5 -> {
                    printOp("TransactionService.finalizeTransaction()", "");
                    System.out.print("  transactionId : ");
                    String txnId = sc.nextLine().trim();
                    if (txnId.isBlank() && activeTxnId != null) {
                        txnId = activeTxnId;
                        System.out.println("  (using active transaction: " + activeTxnId + ")");
                    }
                    System.out.println("  → Inputs: transactionId=" + txnId);

                    boolean ok = txnService.finalizeTransaction(txnId);
                    if (ok) {
                        printPass("Transaction " + txnId + " FINALIZED.");
                        printPass("DB trigger has decremented inventory automatically.");
                        activeTxnId = null;
                    } else {
                        printFail("finalizeTransaction() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 6 -> {
                    printOp("TransactionService.abortTransaction()", "");
                    System.out.print("  transactionId : ");
                    String txnId = sc.nextLine().trim();
                    if (txnId.isBlank() && activeTxnId != null) {
                        txnId = activeTxnId;
                        System.out.println("  (using active transaction: " + activeTxnId + ")");
                    }
                    System.out.println("  → Inputs: transactionId=" + txnId);

                    boolean ok = txnService.abortTransaction(txnId);
                    if (ok) {
                        printPass("Transaction " + txnId + " ABORTED. Inventory unchanged.");
                        activeTxnId = null;
                    } else {
                        printFail("abortTransaction() failed.");
                    }
                }

                // ----------------------------------------------------------
                case 7 -> { printPass("Sales staff logged out."); return; }

                // ----------------------------------------------------------
                case 8 -> {
                    printOp("BillService.printBillToConsole()", "");
                    System.out.print("  transactionId : ");
                    String billTxnId = sc.nextLine().trim();
                    if (billTxnId.isBlank() && activeTxnId != null) {
                        billTxnId = activeTxnId;
                        System.out.println("  (using active/last transaction: " + activeTxnId + ")");
                    }
                    System.out.println("  -> Inputs: transactionId=" + billTxnId);
                    boolean ok = billService.printBillToConsole(billTxnId);
                    if (!ok) printFail("Bill print failed – transaction must be FINALIZED first.");
                }

                default -> printFail("Invalid choice. Enter 1-8.");
            }
        }
    }

    // ================================================================
    // Helper – prints transaction line items and running total
    // ================================================================
    private static void verifyTransactionTotal(String txnId) {
        List<TransactionItem> items = rptService.getSalesLineItemsByTransaction(txnId);
        if (items.isEmpty()) {
            System.out.println("  [Verify] No line items yet for transaction " + txnId);
            return;
        }
        double computed = 0;
        System.out.printf("  [Verify] Line items for %s:%n", txnId);
        System.out.printf("    %-8s %-12s %-8s %-10s %-10s%n",
            "ID", "ItemCode", "Qty", "UnitPrice", "LineTotal");
        System.out.println("    " + "-".repeat(50));
        for (TransactionItem ti : items) {
            computed += ti.getLineTotal();
            System.out.printf("    %-8d %-12s %-8d %-10.2f %-10.2f%n",
                ti.getTransactionItemId(), ti.getItemCode(),
                ti.getQuantity(), ti.getUnitPrice(), ti.getLineTotal());
        }
        System.out.printf("    Computed total: %.2f%n", computed);
    }

    // ================================================================
    // Output formatting helpers
    // ================================================================
    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  Supermarket Automation System - Test Harness  ");
        System.out.println("  Backend Verification CLI  |  JDBC + MySQL      ");
        System.out.println("================================================");
    }

    private static void printSection(String title) {
        System.out.println("\n══════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("══════════════════════════════════════════════");
    }

    private static void printOp(String method, String inputs) {
        System.out.println("\n[TEST] " + method
            + (inputs.isBlank() ? "" : " | " + inputs));
    }

    private static void printPass(String msg) {
        System.out.println("  [PASS] " + msg);
    }

    private static void printFail(String msg) {
        System.out.println("  [FAIL] " + msg);
    }

    private static void printItemHeader() {
        System.out.printf("  %-10s %-25s %-10s %-10s %-8s %-15s%n",
            "Code", "Name", "Price", "CostPrice", "Reorder", "Category");
        System.out.println("  " + "-".repeat(80));
    }

    private static void printItemRow(Item item) {
        System.out.printf("  %-10s %-25s %-10.2f %-10.2f %-8d %-15s%n",
            item.getItemCode(),
            item.getItemName(),
            item.getPrice(),
            item.getCostPrice(),
            item.getReorderLevel(),
            item.getCategory());
    }

    // ================================================================
    // Input helpers
    // ================================================================

    /** Reads an integer safely; loops until valid input. */
    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("  [ERR] Enter a whole number: ");
            }
        }
    }

    /** Reads a double safely; loops until valid input. */
    private static double readDouble() {
        while (true) {
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("  [ERR] Enter a decimal number (e.g. 49.99): ");
            }
        }
    }

    /** Prompts with a label and parses a LocalDateTime. Returns null on bad format. */
    private static LocalDateTime readDateTime(String prompt) {
        System.out.print(prompt);
        try {
            return LocalDateTime.parse(sc.nextLine().trim(), DT_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

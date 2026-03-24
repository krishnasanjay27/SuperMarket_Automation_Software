package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.PriceHistoryDAO;
import model.InventoryRecord;
import model.Item;
import model.PriceHistory;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryService {

    private final ItemDAO            itemDAO;
    private final InventoryRecordDAO inventoryDAO;
    private final PriceHistoryDAO    priceHistoryDAO;

    public InventoryService() {
        this.itemDAO         = new ItemDAO();
        this.inventoryDAO    = new InventoryRecordDAO();
        this.priceHistoryDAO = new PriceHistoryDAO();
    }

    public boolean addNewItem(Item item, int initialStock, String updatedBy) {
        if (item == null) {
            System.err.println("addNewItem() failed – item must not be null.");
            return false;
        }
        if (initialStock < 0) {
            System.err.println("addNewItem() failed – initialStock cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("addNewItem() failed – updatedBy must not be empty.");
            return false;
        }

        boolean itemAdded = itemDAO.addItem(item);
        if (!itemAdded) {
            System.err.println("addNewItem() failed – could not insert item: " + item.getItemCode());
            return false;
        }

        InventoryRecord record = new InventoryRecord();
        record.setItemCode(item.getItemCode());
        record.setStockLevel(initialStock);
        record.setLastUpdated(LocalDateTime.now());
        record.setUpdatedBy(updatedBy);

        boolean inventoryCreated = inventoryDAO.addInventoryRecord(record);
        if (!inventoryCreated) {
            System.err.println("addNewItem() warning – item '" + item.getItemCode()
                               + "' added to catalogue but InventoryRecord creation failed.");
            return false;
        }

        System.out.println("New item '" + item.getItemCode() + "' added with initial stock: " + initialStock);
        return true;
    }

    public boolean updateItemPrice(String itemCode, double newPrice, String updatedBy) {
        if (newPrice < 0) {
            System.err.println("updateItemPrice() failed – price cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("updateItemPrice() failed – updatedBy must not be empty.");
            return false;
        }

        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("updateItemPrice() failed – item not found: " + itemCode);
            return false;
        }

        double oldPrice = item.getPrice();

        if (Double.compare(oldPrice, newPrice) == 0) {
            System.out.println("updateItemPrice() – new price is identical to current price. No change made.");
            return true;
        }

        PriceHistory audit = new PriceHistory();
        audit.setItemCode(itemCode);
        audit.setOldPrice(oldPrice);
        audit.setNewPrice(newPrice);
        audit.setChangedAt(LocalDateTime.now());
        audit.setChangedBy(updatedBy);

        boolean auditSaved = priceHistoryDAO.recordPriceChange(audit);
        if (!auditSaved) {
            System.err.println("updateItemPrice() failed – could not record price history for: " + itemCode);
            return false;
        }

        return itemDAO.updateItemPrice(itemCode, newPrice);
    }

    public boolean updateStock(String itemCode, int quantityChange, String updatedBy) {
        if (quantityChange == 0) {
            System.err.println("updateStock() – quantityChange is 0, no update performed.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("updateStock() failed – updatedBy must not be empty.");
            return false;
        }

        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("updateStock() failed – item not found: " + itemCode);
            return false;
        }

        if (quantityChange < 0) {
            int currentStock = inventoryDAO.getStockLevel(itemCode);
            if (currentStock < 0) {
                System.err.println("updateStock() failed – no inventory record for item: " + itemCode);
                return false;
            }
            if ((currentStock + quantityChange) < 0) {
                System.err.println("updateStock() failed – insufficient stock for '" + itemCode
                                   + "'. Current: " + currentStock
                                   + ", Requested reduction: " + Math.abs(quantityChange));
                return false;
            }
        }

        return inventoryDAO.updateStock(itemCode, quantityChange, updatedBy);
    }

    public boolean setStockLevel(String itemCode, int newStock, String updatedBy) {
        if (newStock < 0) {
            System.err.println("setStockLevel() failed – stock level cannot be negative.");
            return false;
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            System.err.println("setStockLevel() failed – updatedBy must not be empty.");
            return false;
        }

        Item item = itemDAO.getItemByCode(itemCode);
        if (item == null) {
            System.err.println("setStockLevel() failed – item not found: " + itemCode);
            return false;
        }

        return inventoryDAO.setStockLevel(itemCode, newStock, updatedBy);
    }

    public int getStockLevel(String itemCode) {
        return inventoryDAO.getStockLevel(itemCode);
    }

    public Item getItemByCode(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            System.err.println("getItemByCode() failed – itemCode must not be empty.");
            return null;
        }
        return itemDAO.getItemByCode(itemCode);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}

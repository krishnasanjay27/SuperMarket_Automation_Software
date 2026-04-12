package service;

import dao.ItemDAO;
import model.Item;

public class ItemService {

    private final ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    /** Constructor for dependency injection (testing). */
    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    /**
     * Updates an item's details, including its vendorId.
     */
    public boolean updateItemVendor(Item item) {
        if (item == null || item.getItemCode() == null || item.getItemCode().isBlank()) {
            System.err.println("updateItemVendor() failed - valid Item is required.");
            return false;
        }
        return itemDAO.updateItem(item);
    }

    /**
     * Adds an item with a vendor, but hands it off to InventoryService 
     * which handles the critical initial stock allocation natively without modification.
     */
    public boolean addItemWithVendor(Item item, int initialStock, String userId) {
        // Validation check for empty item configuration
        if (item == null) {
            System.err.println("addItemWithVendor() failed - Item must not be null.");
            return false;
        }
        // Since the prompt instructs NOT to modify InventoryService, we delegate
        // the creation workflow to it now that the Item model correctly carries vendorId.
        InventoryService inventoryService = new InventoryService();
        return inventoryService.addNewItem(item, initialStock, userId);
    }
}

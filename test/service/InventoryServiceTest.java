package service;

import dao.InventoryRecordDAO;
import dao.ItemDAO;
import dao.PriceHistoryDAO;
import model.InventoryRecord;
import model.Item;
import model.PriceHistory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InventoryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
class InventoryServiceTest {

    @Mock private ItemDAO            itemDAO;
    @Mock private InventoryRecordDAO inventoryDAO;
    @Mock private PriceHistoryDAO    priceHistoryDAO;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(itemDAO, inventoryDAO, priceHistoryDAO);
    }

    // ─── addNewItem() ─────────────────────────────────────────────

    @Nested
    @DisplayName("addNewItem()")
    class AddNewItemTests {

        @Test
        @DisplayName("Returns false when item is null")
        void addNewItem_nullItem_returnsFalse() {
            assertFalse(inventoryService.addNewItem(null, 10, "INV001"));
        }

        @Test
        @DisplayName("Returns false when initial stock is negative")
        void addNewItem_negativeStock_returnsFalse() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            assertFalse(inventoryService.addNewItem(item, -1, "INV001"));
        }

        @Test
        @DisplayName("Returns false when updatedBy is blank")
        void addNewItem_blankUpdatedBy_returnsFalse() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            assertFalse(inventoryService.addNewItem(item, 10, ""));
        }

        @Test
        @DisplayName("Returns false when DAO addItem fails")
        void addNewItem_daoAddItemFails_returnsFalse() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            when(itemDAO.addItem(item)).thenReturn(false);

            assertFalse(inventoryService.addNewItem(item, 10, "INV001"));
            verify(inventoryDAO, never()).addInventoryRecord(any());
        }

        @Test
        @DisplayName("Creates item and inventory record on success")
        void addNewItem_success_createsItemAndInventory() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            when(itemDAO.addItem(item)).thenReturn(true);
            when(inventoryDAO.addInventoryRecord(any(InventoryRecord.class))).thenReturn(true);

            assertTrue(inventoryService.addNewItem(item, 50, "INV001"));
            verify(itemDAO).addItem(item);
            verify(inventoryDAO).addInventoryRecord(argThat(ir ->
                    ir.getItemCode().equals("ITM099") && ir.getStockLevel() == 50));
        }

        @Test
        @DisplayName("Returns false when inventory record creation fails")
        void addNewItem_inventoryFails_returnsFalse() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            when(itemDAO.addItem(item)).thenReturn(true);
            when(inventoryDAO.addInventoryRecord(any())).thenReturn(false);

            assertFalse(inventoryService.addNewItem(item, 10, "INV001"));
        }

        @Test
        @DisplayName("Zero initial stock is allowed")
        void addNewItem_zeroStock_isAllowed() {
            Item item = new Item("ITM099", "Test", 10.0, 5.0, 5, "Cat", null);
            when(itemDAO.addItem(item)).thenReturn(true);
            when(inventoryDAO.addInventoryRecord(any())).thenReturn(true);

            assertTrue(inventoryService.addNewItem(item, 0, "INV001"));
        }
    }

    // ─── updateItemPrice() ────────────────────────────────────────

    @Nested
    @DisplayName("updateItemPrice()")
    class UpdateItemPriceTests {

        @Test
        @DisplayName("Returns false for negative price")
        void updateItemPrice_negativePrice_returnsFalse() {
            assertFalse(inventoryService.updateItemPrice("ITM001", -5.0, "MGR001"));
        }

        @Test
        @DisplayName("Returns false for blank updatedBy")
        void updateItemPrice_blankUpdatedBy_returnsFalse() {
            assertFalse(inventoryService.updateItemPrice("ITM001", 100.0, ""));
        }

        @Test
        @DisplayName("Returns false when item not found")
        void updateItemPrice_itemNotFound_returnsFalse() {
            when(itemDAO.getItemByCode("NOPE")).thenReturn(null);
            assertFalse(inventoryService.updateItemPrice("NOPE", 100.0, "MGR001"));
        }

        @Test
        @DisplayName("Returns true without change when price is identical")
        void updateItemPrice_samePrice_noChange() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);

            assertTrue(inventoryService.updateItemPrice("ITM001", 299.99, "MGR001"));
            verify(priceHistoryDAO, never()).recordPriceChange(any());
            verify(itemDAO, never()).updateItemPrice(anyString(), anyDouble());
        }

        @Test
        @DisplayName("Records price history and updates price on success")
        void updateItemPrice_differentPrice_recordsHistoryAndUpdates() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(priceHistoryDAO.recordPriceChange(any(PriceHistory.class))).thenReturn(true);
            when(itemDAO.updateItemPrice("ITM001", 349.99)).thenReturn(true);

            assertTrue(inventoryService.updateItemPrice("ITM001", 349.99, "MGR001"));
            verify(priceHistoryDAO).recordPriceChange(argThat(ph ->
                    ph.getOldPrice() == 299.99 && ph.getNewPrice() == 349.99));
            verify(itemDAO).updateItemPrice("ITM001", 349.99);
        }

        @Test
        @DisplayName("Returns false when price history save fails")
        void updateItemPrice_historyFails_returnsFalse() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(priceHistoryDAO.recordPriceChange(any())).thenReturn(false);

            assertFalse(inventoryService.updateItemPrice("ITM001", 350.0, "MGR001"));
            verify(itemDAO, never()).updateItemPrice(anyString(), anyDouble());
        }
    }

    // ─── updateStock() ────────────────────────────────────────────

    @Nested
    @DisplayName("updateStock()")
    class UpdateStockTests {

        @Test
        @DisplayName("Returns false when quantityChange is zero")
        void updateStock_zero_returnsFalse() {
            assertFalse(inventoryService.updateStock("ITM001", 0, "INV001"));
        }

        @Test
        @DisplayName("Returns false when updatedBy is blank")
        void updateStock_blankUpdatedBy_returnsFalse() {
            assertFalse(inventoryService.updateStock("ITM001", 10, " "));
        }

        @Test
        @DisplayName("Returns false when item not found")
        void updateStock_itemNotFound_returnsFalse() {
            when(itemDAO.getItemByCode("NOPE")).thenReturn(null);
            assertFalse(inventoryService.updateStock("NOPE", 10, "INV001"));
        }

        @Test
        @DisplayName("Positive quantity change delegates to DAO without stock check")
        void updateStock_positiveChange_delegatesDirectly() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.updateStock("ITM001", 50, "INV001")).thenReturn(true);

            assertTrue(inventoryService.updateStock("ITM001", 50, "INV001"));
            verify(inventoryDAO, never()).getStockLevel(anyString());
        }

        @Test
        @DisplayName("Negative change fails when no inventory record exists")
        void updateStock_negativeChange_noRecord_returnsFalse() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(-1);

            assertFalse(inventoryService.updateStock("ITM001", -10, "INV001"));
        }

        @Test
        @DisplayName("Negative change fails when insufficient stock")
        void updateStock_negativeChange_insufficientStock_returnsFalse() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(5);

            assertFalse(inventoryService.updateStock("ITM001", -10, "INV001"));
        }

        @Test
        @DisplayName("Negative change succeeds when sufficient stock")
        void updateStock_negativeChange_sufficientStock_delegates() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.getStockLevel("ITM001")).thenReturn(100);
            when(inventoryDAO.updateStock("ITM001", -10, "INV001")).thenReturn(true);

            assertTrue(inventoryService.updateStock("ITM001", -10, "INV001"));
        }
    }

    // ─── setStockLevel() ──────────────────────────────────────────

    @Nested
    @DisplayName("setStockLevel()")
    class SetStockLevelTests {

        @Test
        @DisplayName("Returns false for negative stock level")
        void setStockLevel_negative_returnsFalse() {
            assertFalse(inventoryService.setStockLevel("ITM001", -5, "INV001"));
        }

        @Test
        @DisplayName("Returns false for blank updatedBy")
        void setStockLevel_blankUpdatedBy_returnsFalse() {
            assertFalse(inventoryService.setStockLevel("ITM001", 100, ""));
        }

        @Test
        @DisplayName("Returns false when item not found")
        void setStockLevel_itemNotFound_returnsFalse() {
            when(itemDAO.getItemByCode("NOPE")).thenReturn(null);
            assertFalse(inventoryService.setStockLevel("NOPE", 100, "INV001"));
        }

        @Test
        @DisplayName("Sets stock level on success")
        void setStockLevel_validParams_delegates() {
            Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
            when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
            when(inventoryDAO.setStockLevel("ITM001", 200, "INV001")).thenReturn(true);

            assertTrue(inventoryService.setStockLevel("ITM001", 200, "INV001"));
        }
    }

    // ─── Miscellaneous ────────────────────────────────────────────

    @Test
    @DisplayName("getStockLevel() delegates to DAO")
    void getStockLevel_delegatesToDAO() {
        when(inventoryDAO.getStockLevel("ITM001")).thenReturn(150);
        assertEquals(150, inventoryService.getStockLevel("ITM001"));
    }

    @Test
    @DisplayName("getItemByCode() returns null for blank code")
    void getItemByCode_blankCode_returnsNull() {
        assertNull(inventoryService.getItemByCode(""));
    }

    @Test
    @DisplayName("getItemByCode() returns null for null code")
    void getItemByCode_nullCode_returnsNull() {
        assertNull(inventoryService.getItemByCode(null));
    }

    @Test
    @DisplayName("getAllItems() delegates to DAO")
    void getAllItems_delegatesToDAO() {
        List<Item> items = Arrays.asList(new Item(), new Item());
        when(itemDAO.getAllItems()).thenReturn(items);
        assertEquals(2, inventoryService.getAllItems().size());
    }

    @Test
    @DisplayName("removeItem() returns false for blank itemCode")
    void removeItem_blankCode_returnsFalse() {
        assertFalse(inventoryService.removeItem(""));
    }

    @Test
    @DisplayName("removeItem() returns false when item not found")
    void removeItem_notFound_returnsFalse() {
        when(itemDAO.getItemByCode("NOPE")).thenReturn(null);
        assertFalse(inventoryService.removeItem("NOPE"));
    }

    @Test
    @DisplayName("removeItem() delegates to DAO on success")
    void removeItem_found_delegates() {
        Item item = new Item("ITM001", "Rice", 299.99, 220.0, 20, "Grains", null);
        when(itemDAO.getItemByCode("ITM001")).thenReturn(item);
        when(itemDAO.deleteItemCompletely("ITM001")).thenReturn(true);

        assertTrue(inventoryService.removeItem("ITM001"));
        verify(itemDAO).deleteItemCompletely("ITM001");
    }
}

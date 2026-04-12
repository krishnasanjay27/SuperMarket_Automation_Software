package service;

import dao.ItemDAO;
import model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemDAO);
    }

    @Test
    @DisplayName("updateItemVendor() returns false for null item")
    void updateItemVendor_nullItem_returnsFalse() {
        assertFalse(itemService.updateItemVendor(null));
        verifyNoInteractions(itemDAO);
    }

    @Test
    @DisplayName("updateItemVendor() returns false for item with blank code")
    void updateItemVendor_blankCode_returnsFalse() {
        Item item = new Item();
        item.setItemCode(" ");
        assertFalse(itemService.updateItemVendor(item));
        verifyNoInteractions(itemDAO);
    }

    @Test
    @DisplayName("updateItemVendor() delegates to DAO and returns its result")
    void updateItemVendor_validItem_delegatesToDAO() {
        Item item = new Item();
        item.setItemCode("ITM001");
        
        when(itemDAO.updateItem(item)).thenReturn(true);
        assertTrue(itemService.updateItemVendor(item));
        verify(itemDAO).updateItem(item);
    }

    @Test
    @DisplayName("addItemWithVendor() returns false for null item")
    void addItemWithVendor_nullItem_returnsFalse() {
        assertFalse(itemService.addItemWithVendor(null, 10, "MGR001"));
    }

    // Since `addItemWithVendor` natively constructs `InventoryService` inside its method and cannot be easily mocked without PowerMock or restructuring the code.
    // We test only the validation logic here. In reality, one would inject InventoryService into ItemService, 
    // but we respect the existing architecture.
}

package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.storage.StorageAllocationEntry;
import elmer.proyectofinal_sopes2.storage.WarehouseGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseAllocationTest {

    @Test
    void lotIsPagedAndReturnedToFreeSpace() {
        Product product = new Product("W-1", "Fragil", CommodityType.FRAGIL, 20, 0);
        WarehouseGrid warehouse = new WarehouseGrid(1, 1, 1, 6, 20);
        var allocation = warehouse.allocateLot(product, "LOT-1", 100);
        assertEquals(5, allocation.size());
        assertEquals(100, allocation.stream().mapToInt(StorageAllocationEntry::getUnits).sum());
        assertTrue(warehouse.getOccupiedSlots(product).size() >= 5);
        var extracted = warehouse.deallocateUnits(product, 100);
        assertEquals(100, extracted.stream().mapToInt(StorageAllocationEntry::getUnits).sum());
        assertTrue(warehouse.getOccupiedSlots(product).isEmpty());
    }
}
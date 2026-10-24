package elmer.proyectofinal_sopes2.storage;

import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.StorageSlot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordina el catalogo de productos con el almacen fisico y su stock global.
 */
public final class InventoryManager {

    private final WarehouseGrid warehouse;
    private final ConcurrentMap<String, Product> catalog = new ConcurrentHashMap<>();
    private final ReentrantLock inventoryLock = new ReentrantLock();

   
    public InventoryManager() {
        this(new WarehouseGrid());
    }

  
    public InventoryManager(WarehouseGrid warehouse) {
        this.warehouse = Objects.requireNonNull(warehouse, "El almacen no puede ser nulo");
    }

  
    public Product registerProduct(Product product) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        return catalog.putIfAbsent(product.getId(), product);
    }

  
    public List<StorageAllocationEntry> receiveStock(Product product, String loteId, int units) {
        Product registered = requireRegistered(product);
        inventoryLock.lock();
        try {
            List<StorageAllocationEntry> allocation = warehouse.allocateLot(registered, loteId, units);
            registered.replenishStock(units);
            return allocation;
        } finally {
            inventoryLock.unlock();
        }
    }

   
    public List<StorageAllocationEntry> dispatchStock(Product product, int units) {
        Product registered = requireRegistered(product);
        inventoryLock.lock();
        try {
            if (registered.getStockDisponible() < units) {
                throw new IllegalArgumentException("El stock global es insuficiente");
            }
            List<StorageAllocationEntry> extracted = warehouse.deallocateUnits(registered, units);
            if (!registered.tryReserveStock(units)) {
                throw new IllegalStateException("El stock cambio durante el despacho");
            }
            return extracted;
        } finally {
            inventoryLock.unlock();
        }
    }

   
    public Product findProduct(String productId) {
        return catalog.get(productId);
    }

    
    public int getStock(Product product) {
        return requireRegistered(product).getStockDisponible();
    }

    
    public List<StorageSlot> getOccupiedSlots(Product product) {
        return warehouse.getOccupiedSlots(requireRegistered(product));
    }

   
    public List<Product> getProducts() {
        return List.copyOf(catalog.values());
    }

    
    public WarehouseGrid getWarehouse() {
        return warehouse;
    }

    private Product requireRegistered(Product product) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        Product registered = catalog.get(product.getId());
        if (registered == null) {
            throw new IllegalArgumentException("El producto no esta registrado: " + product.getId());
        }
        return registered;
    }
}
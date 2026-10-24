package elmer.proyectofinal_sopes2.storage;

import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.StorageSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Matriz fisica del almacen modelada como asignacion de memoria no contigua.
 */
public final class WarehouseGrid {

    public static final int DEFAULT_AISLES = 5;
    public static final int DEFAULT_SHELVES = 4;
    public static final int DEFAULT_LEVELS = 3;
    public static final int DEFAULT_SLOTS = 5;
    public static final int DEFAULT_SLOT_CAPACITY = 20;

    private final List<StorageSlot> slots;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public WarehouseGrid() {
        this(DEFAULT_AISLES, DEFAULT_SHELVES, DEFAULT_LEVELS, DEFAULT_SLOTS, DEFAULT_SLOT_CAPACITY);
    }

    public WarehouseGrid(int aisles, int shelves, int levels, int slotCount, int slotCapacity) {
        validateDimension(aisles, "pasillos");
        validateDimension(shelves, "estantes");
        validateDimension(levels, "niveles");
        validateDimension(slotCount, "casillas");
        validateDimension(slotCapacity, "capacidad");

        List<StorageSlot> createdSlots = new ArrayList<>(aisles * shelves * levels * slotCount);
        for (int aisle = 1; aisle <= aisles; aisle++) {
            for (int shelf = 1; shelf <= shelves; shelf++) {
                for (int level = 1; level <= levels; level++) {
                    for (int position = 1; position <= slotCount; position++) {
                        createdSlots.add(new StorageSlot(aisle, shelf, level, position, slotCapacity));
                    }
                }
            }
        }
        slots = Collections.unmodifiableList(createdSlots);
    }

    /**
 * Asigna un lote de forma atomica en casillas disponibles mediante paginacion.
 * @throws IllegalArgumentException Si el lote es invalido o no hay capacidad suficiente.
     */
    public List<StorageAllocationEntry> allocateLot(Product product, String loteId, int units) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        if (loteId == null || loteId.isBlank()) {
            throw new IllegalArgumentException("El lote debe tener un identificador");
        }
        if (units <= 0) {
            throw new IllegalArgumentException("Las unidades deben ser positivas");
        }

        lock.writeLock().lock();
        try {
            List<StorageSlot> candidates = slots.stream()
                    .filter(slot -> slot.isAvailable()
                            && (slot.isEmpty() || product.equals(slot.getProductoActual())))
                    .sorted((first, second) -> Boolean.compare(
                            first.isEmpty(), second.isEmpty()))
                    .toList();
            int freeCapacity = candidates.stream().mapToInt(StorageSlot::getEspacioDisponible).sum();
            if (freeCapacity < units) {
                throw new IllegalArgumentException("No hay espacio suficiente para el lote");
            }

            List<StorageAllocationEntry> allocation = new ArrayList<>();
            int pending = units;
            for (StorageSlot slot : candidates) {
                int stored = slot.allocate(product, loteId, pending);
                if (stored > 0) {
                    allocation.add(new StorageAllocationEntry(slot, product, loteId, stored));
                    pending -= stored;
                }
                if (pending == 0) {
                    break;
                }
            }
            return List.copyOf(allocation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
 * Despacha unidades por criterio first-fit segun el orden fisico.
 * @throws IllegalArgumentException Si la cantidad solicitada excede el inventario total.
     */
    public List<StorageAllocationEntry> deallocateUnits(Product product, int units) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        if (units <= 0) {
            throw new IllegalArgumentException("Las unidades deben ser positivas");
        }

        lock.writeLock().lock();
        try {
            int available = slots.stream()
                    .filter(slot -> product.equals(slot.getProductoActual()))
                    .mapToInt(StorageSlot::getUnidadesOcupadas)
                    .sum();
            if (available < units) {
                throw new IllegalArgumentException("No hay suficiente inventario del producto");
            }

            List<StorageAllocationEntry> extracted = new ArrayList<>();
            int pending = units;
            for (StorageSlot slot : slots) {
                if (!product.equals(slot.getProductoActual())) {
                    continue;
                }
                int quantity = Math.min(pending, slot.getUnidadesOcupadas());
                String loteId = slot.getLoteId();
                int released = slot.release(quantity);
                extracted.add(new StorageAllocationEntry(slot, product, loteId, released));
                pending -= released;
                if (pending == 0) {
                    break;
                }
            }
            return List.copyOf(extracted);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<StorageSlot> getSlots() {
        lock.readLock().lock();
        try {
            return List.copyOf(slots);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<StorageSlot> getOccupiedSlots(Product product) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        lock.readLock().lock();
        try {
            return slots.stream()
                    .filter(slot -> product.equals(slot.getProductoActual()))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTotalCapacity() {
        return slots.stream().mapToInt(StorageSlot::getCapacidadMaxima).sum();
    }

    private static void validateDimension(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("La dimension de " + name + " debe ser positiva");
        }
    }
}
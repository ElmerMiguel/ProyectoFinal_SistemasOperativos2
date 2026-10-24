package elmer.proyectofinal_sopes2.storage;

import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.StorageSlot;

import java.util.Objects;

/**
 * Entrada de la tabla de paginas para una fraccion de lote almacenada.
 */
public final class StorageAllocationEntry {

    private final StorageSlot slot;
    private final Product product;
    private final String loteId;
    private final int units;

    StorageAllocationEntry(StorageSlot slot, Product product, String loteId, int units) {
        this.slot = Objects.requireNonNull(slot, "La casilla no puede ser nula");
        this.product = Objects.requireNonNull(product, "El producto no puede ser nulo");
        this.loteId = Objects.requireNonNull(loteId, "El lote no puede ser nulo");
        if (units <= 0) {
            throw new IllegalArgumentException("Las unidades deben ser positivas");
        }
        this.units = units;
    }

 
    public StorageSlot getSlot() {
        return slot;
    }

    public Product getProduct() {
        return product;
    }

  
    public String getLoteId() {
        return loteId;
    }

    public int getUnits() {
        return units;
    }

    @Override
    public String toString() {
        return slot.getUbicacionCodigo() + " -> " + units + " unidades (" + loteId + ")";
    }
}
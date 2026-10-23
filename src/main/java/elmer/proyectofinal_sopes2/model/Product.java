package elmer.proyectofinal_sopes2.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Product {

    private final String id;
    private final String nombre;
    private final CommodityType tipoMercancia;
    private final int unidadesPorSlot;
    private final AtomicInteger stockDisponible;

    public Product(String id, String nombre, CommodityType tipoMercancia, int unidadesPorSlot, int stockInicial) {
        this.id = Objects.requireNonNull(id, "El ID del producto no puede ser nulo");
        this.nombre = Objects.requireNonNull(nombre, "El nombre del producto no puede ser nulo");
        this.tipoMercancia = Objects.requireNonNull(tipoMercancia, "El tipo de mercancía no puede ser nulo");
        this.unidadesPorSlot = Math.max(1, unidadesPorSlot);
        this.stockDisponible = new AtomicInteger(Math.max(0, stockInicial));
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public CommodityType getTipoMercancia() {
        return tipoMercancia;
    }

    public int getUnidadesPorSlot() {
        return unidadesPorSlot;
    }

    public int getStockDisponible() {
        return stockDisponible.get();
    }

    /**
     * reserv stock usando CAS.
     *
     * @param cantidad Cantidad a reservar.
     * @return true si tiene exito; false en caso contrario.
     */
    public boolean tryReserveStock(int cantidad) {
        if (cantidad <= 0) {
            return true;
        }
        int actual;
        do {
            actual = stockDisponible.get();
            if (actual < cantidad) {
                return false;
            }
        } while (!stockDisponible.compareAndSet(actual, actual - cantidad));
        return true;
    }

    /**
     * Incrementa atómicamente el stock disponible tras la recepción de nuevo
     * inventario.
     *
     * @param cantidad Cantidad de unidades añadidas
     * @return Nuevo stock disponible total
     */
    public int replenishStock(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad a reponer no puede ser negativa: " + cantidad);
        }
        return stockDisponible.addAndGet(cantidad);
    }

    /**
     * Restaura unidades que fueron reservadas previamente si una orden es
     * cancelada o rollbackeada.
     *
     * @param cantidad Cantidad a devolver
     */
    public void rollbackStock(int cantidad) {
        if (cantidad > 0) {
            stockDisponible.addAndGet(cantidad);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " - " + nombre + " [" + tipoMercancia.getDisplayName() + "] (Stock: " + stockDisponible.get() + ")";
    }
}

package elmer.proyectofinal_sopes2.model;

import java.util.Objects;


public class OrderItem {

    private final Product product;
    private final int cantidad;

    public OrderItem(Product product, int cantidad) {
        this.product = Objects.requireNonNull(product, "El producto no puede ser nulo");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0: " + cantidad);
        }
        this.cantidad = cantidad;
    }

    public Product getProduct() {
        return product;
    }

    public int getCantidad() {
        return cantidad;
    }

    public CommodityType getCommodityType() {
        return product.getTipoMercancia();
    }

    @Override
    public String toString() {
        return product.getNombre() + " x" + cantidad + " (" + product.getTipoMercancia().getDisplayName() + ")";
    }
}

package elmer.proyectofinal_sopes2.ipc;

import elmer.proyectofinal_sopes2.model.Product;

import java.util.Objects;

/**
 * Mensaje inmutable que representa un lote recibido por Recepcion.
 */
public record StockArrivalBatch(String batchId, long timestampLlegada, Product producto,
                                int cantidadRecibida, String camionOrigen) {

    public StockArrivalBatch {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("El lote debe tener un identificador");
        }
        Objects.requireNonNull(producto, "El producto no puede ser nulo");
        if (cantidadRecibida <= 0) {
            throw new IllegalArgumentException("La cantidad recibida debe ser positiva");
        }
        if (camionOrigen == null || camionOrigen.isBlank()) {
            throw new IllegalArgumentException("El camion de origen es obligatorio");
        }
    }
}
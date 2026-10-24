package elmer.proyectofinal_sopes2.ipc;

import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.storage.InventoryManager;

import java.util.Objects;

/**
 * Consumidor que transforma mensajes IPC en inventario fisico y stock global.
 */
public final class InventoryWorker implements Runnable {

    private final ReceptionBuffer buffer;
    private final InventoryManager inventoryManager;
    private final InventoryReevaluationListener reevaluationListener;
    private final long processingDelayMs;

  
    public InventoryWorker(ReceptionBuffer buffer, InventoryManager inventoryManager,
                           InventoryReevaluationListener reevaluationListener, long processingDelayMs) {
        this.buffer = Objects.requireNonNull(buffer, "El buffer no puede ser nulo");
        this.inventoryManager = Objects.requireNonNull(inventoryManager, "El inventario no puede ser nulo");
        this.reevaluationListener = Objects.requireNonNull(reevaluationListener, "El listener es obligatorio");
        if (processingDelayMs < 0) {
            throw new IllegalArgumentException("El retraso no puede ser negativo");
        }
        this.processingDelayMs = processingDelayMs;
    }

   
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processNext();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

   
    public void processNext() throws InterruptedException {
        StockArrivalBatch batch = buffer.take();
        if (processingDelayMs > 0) {
            Thread.sleep(processingDelayMs);
        }
        Product product = batch.producto();
        inventoryManager.receiveStock(product, batch.batchId(), batch.cantidadRecibida());
        reevaluationListener.onInventoryUpdated(product, batch.cantidadRecibida());
    }
}
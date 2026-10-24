package elmer.proyectofinal_sopes2.ipc;

import elmer.proyectofinal_sopes2.concurrency.ResourceAllocation;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Productor que escanea manifiestos y publica lotes en el buffer acotado.
 */
public final class ReceptionWorker implements Runnable {

    private final ReceptionBuffer buffer;
    private final ResourceManager resourceManager;
    private final Order scannerOwner;
    private final Supplier<StockArrivalBatch> batchSupplier;
    private final long intervalMs;

    /**
     * Crea un productor automatico; el escaner se libera siempre despues del manifiesto.
     */
    public ReceptionWorker(ReceptionBuffer buffer, ResourceManager resourceManager, Order scannerOwner,
                           Supplier<StockArrivalBatch> batchSupplier, long intervalMs) {
        this.buffer = Objects.requireNonNull(buffer, "El buffer no puede ser nulo");
        this.resourceManager = Objects.requireNonNull(resourceManager, "El gestor no puede ser nulo");
        this.scannerOwner = Objects.requireNonNull(scannerOwner, "El propietario del escaner es obligatorio");
        this.batchSupplier = Objects.requireNonNull(batchSupplier, "El generador no puede ser nulo");
        if (intervalMs < 0) {
            throw new IllegalArgumentException("El intervalo no puede ser negativo");
        }
        this.intervalMs = intervalMs;
    }

    /**
     * Simula llegadas periodicas; una interrupcion termina el productor y restaura su estado.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                StockArrivalBatch batch = batchSupplier.get();
                if (batch != null) {
                    receive(batch);
                }
                if (intervalMs > 0) {
                    Thread.sleep(intervalMs);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }


    public void receive(StockArrivalBatch batch) throws InterruptedException {
        Objects.requireNonNull(batch, "El lote no puede ser nulo");
        ResourceAllocation allocation = resourceManager.acquireResources(
                scannerOwner, Map.of(ResourceType.SISTEMA_ESCANEO, 1), 5_000);
        if (allocation == null) {
            throw new IllegalStateException("El sistema de escaneo no esta disponible");
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(1, 5));
        } finally {
            resourceManager.releaseResources(allocation);
        }
        buffer.put(batch);
    }
}
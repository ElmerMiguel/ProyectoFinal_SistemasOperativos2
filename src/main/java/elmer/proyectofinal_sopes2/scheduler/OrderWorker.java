package elmer.proyectofinal_sopes2.scheduler;

import elmer.proyectofinal_sopes2.concurrency.ResourceAllocation;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.storage.InventoryManager;

import java.util.Objects;

/** Procesa pedidos elegibles sin bloquear la cola cuando falta un recurso. */
public final class OrderWorker implements Runnable {

    private final OrderScheduler scheduler;
    private final ResourceManager resourceManager;
    private final InventoryManager inventoryManager;
    private final long resourceTimeoutMs;

    public OrderWorker(OrderScheduler scheduler, ResourceManager resourceManager,
                       InventoryManager inventoryManager, long resourceTimeoutMs) {
        this.scheduler = Objects.requireNonNull(scheduler, "El scheduler no puede ser nulo");
        this.resourceManager = Objects.requireNonNull(resourceManager, "El gestor no puede ser nulo");
        this.inventoryManager = Objects.requireNonNull(inventoryManager, "El inventario no puede ser nulo");
        if (resourceTimeoutMs < 0) {
            throw new IllegalArgumentException("El timeout no puede ser negativo");
        }
        this.resourceTimeoutMs = resourceTimeoutMs;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processNext(scheduler.takeNext(250));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** Intenta un pedido y lo devuelve a la cola si sus recursos siguen ocupados. */
    public boolean processNext(Order order) throws InterruptedException {
        if (order == null) {
            return false;
        }
        ResourceAllocation allocation = resourceManager.acquireResources(
                order, order.getTipoMercanciaPrincipal().getRequiredResources(), resourceTimeoutMs);
        if (allocation == null) {
            order.setEstado(OrderStatus.EN_ESPERA_RECURSOS);
            scheduler.submitOrder(order);
            return false;
        }
        long start = System.currentTimeMillis();
        order.setTiempoInicioAtencion(start);
        order.setEstado(OrderStatus.EN_PROCESO);
        try {
            Thread.sleep(order.getTiempoTrabajoEstimadoMs());
            for (var item : order.getItems()) {
                inventoryManager.dispatchReservedStock(item.getProduct(), item.getCantidad());
            }
            order.setTiempoFinalizacion(System.currentTimeMillis());
            order.setEstado(OrderStatus.COMPLETADO);
            return true;
        } catch (InterruptedException exception) {
            order.setEstado(OrderStatus.CANCELADO);
            releaseReservedStock(order);
            Thread.currentThread().interrupt();
            throw exception;
        } catch (RuntimeException exception) {
            releaseReservedStock(order);
            throw exception;
        } finally {
            resourceManager.releaseResources(allocation);
        }
    }

    private void releaseReservedStock(Order order) {
        order.getItems().forEach(item ->
                inventoryManager.releaseReservedStock(item.getProduct(), item.getCantidad()));
    }
}
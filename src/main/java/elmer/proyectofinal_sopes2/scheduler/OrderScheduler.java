package elmer.proyectofinal_sopes2.scheduler;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.storage.InventoryManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Coordina las colas de pedidos y permite cambiar la politica durante la simulacion. */
public final class OrderScheduler {

    public static final double DEFAULT_AGING_FACTOR = 0.001;

    private final InventoryManager inventoryManager;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition orderAvailable = lock.newCondition();
    private final List<Order> readyOrders = new ArrayList<>();
    private final List<Order> waitingForInventory = new ArrayList<>();
    private final List<Order> activeOrders = new ArrayList<>();
    private SchedulingPolicy policy;
    private double agingFactor;

    public OrderScheduler(InventoryManager inventoryManager) {
        this(inventoryManager, SchedulingPolicy.PRIORITY_STRICT, DEFAULT_AGING_FACTOR);
    }

    public OrderScheduler(InventoryManager inventoryManager, SchedulingPolicy policy, double agingFactor) {
        this.inventoryManager = Objects.requireNonNull(inventoryManager, "El inventario no puede ser nulo");
        this.policy = Objects.requireNonNull(policy, "La politica no puede ser nula");
        if (agingFactor < 0) {
            throw new IllegalArgumentException("El factor de aging no puede ser negativo");
        }
        this.agingFactor = agingFactor;
    }

    /** La comprobacion de stock ocurre antes de publicar el pedido en la cola elegible. */
    public void submitOrder(Order order) {
        Objects.requireNonNull(order, "El pedido no puede ser nulo");
        lock.lock();
        try {
            if (hasStock(order)) {
                reserveOrder(order);
                order.setEstado(OrderStatus.EN_ESPERA_RECURSOS);
                readyOrders.add(order);
                sortReadyOrders();
                orderAvailable.signal();
            } else {
                order.setEstado(OrderStatus.EN_ESPERA_INVENTARIO);
                waitingForInventory.add(order);
            }
        } finally {
            lock.unlock();
        }
    }

    /** Cambiar la politica reordena inmediatamente la cola sin perder pedidos. */
    public void setPolicy(SchedulingPolicy newPolicy) {
        lock.lock();
        try {
            policy = Objects.requireNonNull(newPolicy, "La politica no puede ser nula");
            sortReadyOrders();
            orderAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Mueve pedidos con stock disponible a la cola activa y despierta workers. */
    public void reevaluateWaitingOrders() {
        lock.lock();
        try {
            waitingForInventory.removeIf(order -> {
                if (!hasStock(order)) {
                    return false;
                }
                reserveOrder(order);
                order.setEstado(OrderStatus.EN_ESPERA_RECURSOS);
                readyOrders.add(order);
                return true;
            });
            sortReadyOrders();
            orderAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Actualiza prioridad visible para aging y vuelve a ordenar la cola. */
    public void ageOrders(long elapsedSimTime) {
        if (elapsedSimTime < 0) {
            throw new IllegalArgumentException("El tiempo simulado no puede ser negativo");
        }
        lock.lock();
        try {
            readyOrders.forEach(order -> order.setPrioridadEfectiva(
                    order.getServiceLevel().getBasePriority() + elapsedSimTime * agingFactor));
            sortReadyOrders();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Toma el siguiente pedido sin bloquear indefinidamente al worker.
     *
     * @return Pedido elegible o null si expira el timeout.
     */
    public Order takeNext(long timeoutMs) throws InterruptedException {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("El timeout no puede ser negativo");
        }
        long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        lock.lockInterruptibly();
        try {
            while (readyOrders.isEmpty()) {
                if (remaining <= 0) {
                    return null;
                }
                remaining = orderAvailable.awaitNanos(remaining);
            }
            return readyOrders.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public SchedulingPolicy getPolicy() {
        lock.lock();
        try {
            return policy;
        } finally {
            lock.unlock();
        }
    }

    public List<Order> getReadyOrders() {
        lock.lock();
        try {
            return List.copyOf(readyOrders);
        } finally {
            lock.unlock();
        }
    }

    public List<Order> getWaitingForInventory() {
        lock.lock();
        try {
            return List.copyOf(waitingForInventory);
        } finally {
            lock.unlock();
        }
    }

    public void registerActive(Order order) {
        if (order == null) return;
        lock.lock();
        try {
            activeOrders.add(order);
        } finally {
            lock.unlock();
        }
    }

    public void unregisterActive(Order order) {
        if (order == null) return;
        lock.lock();
        try {
            activeOrders.remove(order);
        } finally {
            lock.unlock();
        }
    }

    public List<Order> getActiveOrders() {
        lock.lock();
        try {
            return List.copyOf(activeOrders);
        } finally {
            lock.unlock();
        }
    }

    private boolean hasStock(Order order) {
        return order.getItems().stream().allMatch(this::hasStock);
    }

    private boolean hasStock(OrderItem item) {
        return inventoryManager.findProduct(item.getProduct().getId()) != null
                && inventoryManager.getStock(item.getProduct()) >= item.getCantidad();
    }

    private void sortReadyOrders() {
        Comparator<Order> comparator = new OrderComparator(policy, System.currentTimeMillis(), agingFactor);
        readyOrders.sort(comparator);
    }

    private void reserveOrder(Order order) {
        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                inventoryManager.reserveStock(item.getProduct(), item.getCantidad());
                reserved.add(item);
            }
        } catch (RuntimeException exception) {
            reserved.forEach(item -> inventoryManager.releaseReservedStock(item.getProduct(), item.getCantidad()));
            throw exception;
        }
    }
}
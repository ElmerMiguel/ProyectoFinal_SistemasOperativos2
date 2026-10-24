package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Objects;

/**
 * Representa una unidad fisica trazable de un recurso compartido.
 */
public final class ResourceInstance {

    private final String id;
    private final ResourceType type;
    private boolean inUse;
    private Order occupiedBy;
    private long usageStartTime;

    ResourceInstance(String id, ResourceType type) {
        this.id = Objects.requireNonNull(id, "El ID del recurso no puede ser nulo");
        this.type = Objects.requireNonNull(type, "El tipo de recurso no puede ser nulo");
    }

    /**
     * @return Identificador unico de la instancia fisica.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Tipo de recurso al que pertenece la instancia.
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * @return true cuando la instancia esta asignada a una operacion.
     */
    public synchronized boolean isInUse() {
        return inUse;
    }

    /**
     * @return Pedido que posee la instancia o null si esta libre.
     */
    public synchronized Order getOccupiedBy() {
        return occupiedBy;
    }

    /**
     * @return Momento de inicio de uso o cero si esta libre.
     */
    public synchronized long getUsageStartTime() {
        return usageStartTime;
    }

    synchronized void assignTo(Order order, long startTime) {
        if (inUse) {
            throw new IllegalStateException("El recurso ya esta ocupado: " + id);
        }
        inUse = true;
        occupiedBy = Objects.requireNonNull(order, "El pedido no puede ser nulo");
        usageStartTime = startTime;
    }

    synchronized void releaseFrom(Order order) {
        if (!inUse || !Objects.equals(occupiedBy, order)) {
            throw new IllegalArgumentException("El pedido no posee el recurso: " + id);
        }
        inUse = false;
        occupiedBy = null;
        usageStartTime = 0L;
    }
}
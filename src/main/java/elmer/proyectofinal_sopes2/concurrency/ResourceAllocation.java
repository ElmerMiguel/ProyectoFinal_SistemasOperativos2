package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registro inmutable de los recursos reservados por un pedido.
 */
public final class ResourceAllocation {

    private final Order order;
    private final Map<ResourceType, List<ResourceInstance>> instances;

    ResourceAllocation(Order order, Map<ResourceType, List<ResourceInstance>> instances) {
        this.order = Objects.requireNonNull(order, "El pedido no puede ser nulo");
        EnumMap<ResourceType, List<ResourceInstance>> copy = new EnumMap<>(ResourceType.class);
        instances.forEach((type, values) -> copy.put(type, List.copyOf(values)));
        this.instances = Collections.unmodifiableMap(copy);
    }

    /**
     * @return Pedido propietario de la asignacion.
     */
    public Order getOrder() {
        return order;
    }

    /**
     * @return Mapa inmutable con las instancias fisicas asignadas por tipo.
     */
    public Map<ResourceType, List<ResourceInstance>> getInstances() {
        return instances;
    }

    /**
     * @return Cantidad total de instancias reservadas.
     */
    public int getTotalInstances() {
        return instances.values().stream().mapToInt(List::size).sum();
    }
}
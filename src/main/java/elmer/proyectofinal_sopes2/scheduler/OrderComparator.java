package elmer.proyectofinal_sopes2.scheduler;

import elmer.proyectofinal_sopes2.model.Order;

import java.util.Comparator;
import java.util.Objects;

/** Comparador que recalcula el orden con la politica y el reloj actuales. */
public final class OrderComparator implements Comparator<Order> {

    private final SchedulingPolicy policy;
    private final long currentTime;
    private final double agingFactor;

    public OrderComparator(SchedulingPolicy policy, long currentTime, double agingFactor) {
        this.policy = Objects.requireNonNull(policy, "La politica no puede ser nula");
        if (agingFactor < 0) {
            throw new IllegalArgumentException("El factor de aging no puede ser negativo");
        }
        this.currentTime = currentTime;
        this.agingFactor = agingFactor;
    }

    @Override
    public int compare(Order first, Order second) {
        int result = switch (policy) {
            case PRIORITY_STRICT -> Integer.compare(
                    second.getServiceLevel().getBasePriority(), first.getServiceLevel().getBasePriority());
            case AGING -> Double.compare(effectivePriority(second), effectivePriority(first));
            case SHORTEST_JOB_FIRST -> Integer.compare(
                    first.getCantidadTotalUnidades(), second.getCantidadTotalUnidades());
            case FIFO -> Long.compare(first.getTiempoLlegada(), second.getTiempoLlegada());
        };
        if (result != 0) {
            return result;
        }
        return Long.compare(first.getTiempoLlegada(), second.getTiempoLlegada());
    }

    private double effectivePriority(Order order) {
        return order.getServiceLevel().getBasePriority()
                + (order.getTiempoEspera(currentTime) * agingFactor);
    }
}
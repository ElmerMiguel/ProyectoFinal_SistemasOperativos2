package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.OrderComparator;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerPolicyTest {

    private final Client client = new Client("C", "Cliente", ServiceLevel.NIVEL_1_EXPRES);
    private final Product product = new Product("P", "Producto", CommodityType.ESTANDAR, 20, 0);

    @Test
    void strictPriorityPutsExpressFirst() {
        Order express = order("E", ServiceLevel.NIVEL_1_EXPRES, 50, 2);
        Order economy = order("K", ServiceLevel.NIVEL_5_ECONOMICO, 1, 1);
        assertTrue(new OrderComparator(SchedulingPolicy.PRIORITY_STRICT, 100, 0).compare(express, economy) < 0);
    }

    @Test
    void shortestJobPutsSmallOrderFirst() {
        Order small = order("S", ServiceLevel.NIVEL_5_ECONOMICO, 5, 1);
        Order large = order("L", ServiceLevel.NIVEL_2_PRIORITARIO, 50, 2);
        assertTrue(new OrderComparator(SchedulingPolicy.SHORTEST_JOB_FIRST, 100, 0).compare(small, large) < 0);
    }

    @Test
    void agingCanOvertakeHigherBasePriority() {
        Order old = order("O", ServiceLevel.NIVEL_5_ECONOMICO, 1, 0);
        Order recent = order("N", ServiceLevel.NIVEL_4_PROGRAMADO, 1, 99_900);
        assertTrue(new OrderComparator(SchedulingPolicy.AGING, 100_000, 0.001).compare(old, recent) < 0);
    }

    @Test
    void fifoUsesArrivalTime() {
        Order first = order("F", ServiceLevel.NIVEL_5_ECONOMICO, 1, 10);
        Order second = order("G", ServiceLevel.NIVEL_1_EXPRES, 1, 20);
        assertTrue(new OrderComparator(SchedulingPolicy.FIFO, 100, 0).compare(first, second) < 0);
    }

    private Order order(String id, ServiceLevel level, int units, long arrival) {
        return new Order(id, client, level, List.of(new OrderItem(product, units)), arrival);
    }
}
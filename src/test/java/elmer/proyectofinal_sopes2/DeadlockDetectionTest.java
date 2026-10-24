package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.concurrency.DeadlockDetector;
import elmer.proyectofinal_sopes2.concurrency.DeadlockIncident;
import elmer.proyectofinal_sopes2.concurrency.DeadlockScenario;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.concurrency.WaitForGraph;
import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeadlockDetectionTest {

    @Test
    void deliberateCycleCanBeResolved() throws Exception {
        var client = new Client("DL-C", "Deadlock", ServiceLevel.NIVEL_1_EXPRES);
        var product = new Product("DL-P", "Carga", CommodityType.ESTANDAR, 20, 0);
        Order first = order("DL-A", client, product);
        Order second = order("DL-B", client, product);
        WaitForGraph graph = new WaitForGraph();
        DeadlockScenario scenario = new DeadlockScenario(new ResourceManager(), new DeadlockDetector(graph));
        DeadlockIncident incident = scenario.triggerDeliberateDeadlock(first, second);
        assertNotNull(incident);
        assertFalse(graph.findCycle().isEmpty());
        assertEquals(OrderStatus.BLOQUEADO_DEADLOCK, first.getEstado());
        scenario.resolve(incident, first);
        assertEquals(List.of(), graph.findCycle());
        assertEquals(OrderStatus.EN_ESPERA_RECURSOS, second.getEstado());
    }

    private static Order order(String id, Client client, Product product) {
        return new Order(id, client, ServiceLevel.NIVEL_1_EXPRES, List.of(new OrderItem(product, 1)), 0);
    }
}
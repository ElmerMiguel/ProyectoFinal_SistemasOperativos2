package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.concurrency.ResourceAllocation;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyTest {

    @Test
    void resourceCapacityIsNeverExceeded() throws Exception {
        ResourceManager manager = new ResourceManager();
        ExecutorService pool = Executors.newFixedThreadPool(30);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        for (int i = 0; i < 30; i++) {
            int index = i;
            pool.submit(() -> {
                start.await();
                Order order = order("R-" + index);
                ResourceAllocation allocation = manager.acquireResources(order,
                        java.util.Map.of(ResourceType.ESTACION_EMPAQUE, 1), 2_000);
                assertTrue(allocation != null);
                int now = active.incrementAndGet();
                maximum.accumulateAndGet(now, Math::max);
                Thread.sleep(10);
                try {
                    manager.releaseResources(allocation);
                } finally {
                    active.decrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(maximum.get() <= ResourceType.ESTACION_EMPAQUE.getDefaultCapacity());
        assertEquals(6, manager.getAvailableCount(ResourceType.ESTACION_EMPAQUE));
    }

    private static Order order(String id) {
        var product = new elmer.proyectofinal_sopes2.model.Product(id, "P", CommodityType.ESTANDAR, 20, 0);
        return new Order(id, new Client("C-" + id, "C", ServiceLevel.NIVEL_1_EXPRES),
                ServiceLevel.NIVEL_1_EXPRES, List.of(new OrderItem(product, 1)), 0);
    }
}
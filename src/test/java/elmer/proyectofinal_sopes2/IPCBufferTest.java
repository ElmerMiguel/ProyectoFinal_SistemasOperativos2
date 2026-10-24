package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.ipc.ReceptionBuffer;
import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Product;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IPCBufferTest {

    @Test
    void boundedBufferProcessesEveryBatch() throws Exception {
        ReceptionBuffer buffer = new ReceptionBuffer(10);
        Product product = new Product("IPC-T", "Producto", CommodityType.ESTANDAR, 20, 0);
        AtomicInteger processed = new AtomicInteger();
        CountDownLatch producerDone = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    buffer.put(new StockArrivalBatch("LOT-" + i, i, product, 1, "TRUCK"));
                }
                producerDone.countDown();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        pool.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    buffer.take();
                    processed.incrementAndGet();
                    Thread.sleep(2);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(producerDone.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(20, processed.get());
        assertEquals(0, buffer.size());
    }
}
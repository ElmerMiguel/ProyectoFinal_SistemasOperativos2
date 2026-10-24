package elmer.proyectofinal_sopes2.simulation;

import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.ipc.DefaultProductCatalog;
import elmer.proyectofinal_sopes2.ipc.InventoryWorker;
import elmer.proyectofinal_sopes2.ipc.ReceptionBuffer;
import elmer.proyectofinal_sopes2.ipc.ReceptionWorker;
import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.OrderScheduler;
import elmer.proyectofinal_sopes2.scheduler.OrderWorker;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import elmer.proyectofinal_sopes2.storage.InventoryManager;
import elmer.proyectofinal_sopes2.storage.WarehouseGrid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Fachada que levanta y detiene los workers de la simulacion. */
public final class SimulationController {

    private final SimulationClock clock = new SimulationClock();
    private final ResourceManager resourceManager = new ResourceManager();
    private final InventoryManager inventoryManager = new InventoryManager(new WarehouseGrid());
    private final OrderScheduler scheduler = new OrderScheduler(inventoryManager);
    private final MetricsCollector metrics = new MetricsCollector();
    private final ReceptionBuffer receptionBuffer = new ReceptionBuffer();
    private final List<Product> products;
    private final CopyOnWriteArrayList<Thread> threads = new CopyOnWriteArrayList<>();
    private final ReceptionWorker receptionWorker;
    private final InventoryWorker inventoryWorker;
    private final OrderGenerator orderGenerator;
    private volatile boolean running;

    public SimulationController() {
        products = DefaultProductCatalog.create();
        products.forEach(inventoryManager::registerProduct);
        Client scannerClient = new Client("SYS", "Sistema", ServiceLevel.NIVEL_1_EXPRES);
        Order scannerOwner = new Order("SCANNER-OWNER", scannerClient,
                ServiceLevel.NIVEL_1_EXPRES, List.of(new OrderItem(products.get(0), 1)), 0);
        receptionWorker = new ReceptionWorker(receptionBuffer, resourceManager, scannerOwner,
                () -> null, 0);
        inventoryWorker = new InventoryWorker(receptionBuffer, inventoryManager,
                (product, units) -> scheduler.reevaluateWaitingOrders(), 1);
        orderGenerator = new OrderGenerator(clock, scheduler, products, List.of(), 0.5,
                ignored -> metrics.recordGenerated());
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        clock.resume();
        startThread(inventoryWorker, "inventory-worker");
        startThread(orderGenerator, "order-generator");
        for (int i = 0; i < 2; i++) {
            OrderWorker worker = new OrderWorker(scheduler, resourceManager, inventoryManager, 100);
            startThread(() -> runOrderWorker(worker), "order-worker-" + i);
        }
    }

    public synchronized void pause() {
        clock.pause();
    }

    public synchronized void stop() {
        running = false;
        clock.pause();
        threads.forEach(Thread::interrupt);
        threads.clear();
    }

    public synchronized void reset() {
        stop();
        metrics.resetMetrics();
        clock.setSpeedMultiplier(1.0);
        clock.resume();
    }

    public void createManualOrder(Order order) {
        scheduler.submitOrder(Objects.requireNonNull(order, "El pedido no puede ser nulo"));
        metrics.recordGenerated();
    }

    public void receiveManualStock(StockArrivalBatch batch) throws InterruptedException {
        receptionWorker.receive(Objects.requireNonNull(batch, "El lote no puede ser nulo"));
    }

    public SimulationClock getClock() { return clock; }
    public MetricsCollector getMetrics() { return metrics; }
    public OrderScheduler getScheduler() { return scheduler; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public ResourceManager getResourceManager() { return resourceManager; }
    public ReceptionBuffer getReceptionBuffer() { return receptionBuffer; }
    public List<Product> getProducts() { return products; }

    private void runOrderWorker(OrderWorker worker) {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                Order order = scheduler.takeNext(250);
                if (worker.processNext(order)) {
                    metrics.recordCompleted(order, scheduler.getPolicy());
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void startThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        threads.add(thread);
        thread.start();
    }

    private static final class DefaultProducts {
        private static List<Product> create() {
            return List.of(
                    new Product("SIM-01", "Vajilla", elmer.proyectofinal_sopes2.model.CommodityType.FRAGIL, 20, 0),
                    new Product("SIM-02", "Camisetas", elmer.proyectofinal_sopes2.model.CommodityType.ESTANDAR, 20, 0),
                    new Product("SIM-03", "Smartphone", elmer.proyectofinal_sopes2.model.CommodityType.ELECTRONICA_VALOR, 20, 0),
                    new Product("SIM-04", "Cemento", elmer.proyectofinal_sopes2.model.CommodityType.BULK_PALETIZADO, 20, 0),
                    new Product("SIM-05", "Motor", elmer.proyectofinal_sopes2.model.CommodityType.PESADO, 20, 0));
        }
    }
}
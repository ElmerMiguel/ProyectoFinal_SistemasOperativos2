package elmer.proyectofinal_sopes2.simulation;

import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.OrderScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Genera pedidos aleatorios a una tasa expresada en pedidos por segundo virtual. */
public final class OrderGenerator implements Runnable {

    private final SimulationClock clock;
    private final OrderScheduler scheduler;
    private final List<Product> products;
    private final List<Client> clients;
    private final double ordersPerSecond;
    private final Consumer<Order> generatedListener;
    private final AtomicLong clientSequence = new AtomicLong(1);

    public OrderGenerator(SimulationClock clock, OrderScheduler scheduler, List<Product> products,
                          List<Client> clients, double ordersPerSecond, Consumer<Order> generatedListener) {
        this.clock = Objects.requireNonNull(clock, "El reloj no puede ser nulo");
        this.scheduler = Objects.requireNonNull(scheduler, "El scheduler no puede ser nulo");
        this.products = List.copyOf(products);
        this.clients = Objects.requireNonNull(clients, "La lista de clientes no puede ser nula");
        if (this.products.isEmpty() || ordersPerSecond <= 0) {
            throw new IllegalArgumentException("Productos y tasa deben ser positivos");
        }
        this.ordersPerSecond = ordersPerSecond;
        this.generatedListener = Objects.requireNonNull(generatedListener, "El listener no puede ser nulo");
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Order order = generateOrder();
                scheduler.submitOrder(order);
                generatedListener.accept(order);
                clock.sleepVirtual(Math.max(1L, (long) (1_000 / ordersPerSecond)));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private Order generateOrder() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Client client = clients.isEmpty() || random.nextBoolean()
                ? new Client("CLI-AUTO-" + clientSequence.getAndIncrement(), "Cliente automatico",
                ServiceLevel.values()[random.nextInt(ServiceLevel.values().length)])
                : clients.get(random.nextInt(clients.size()));
        ServiceLevel level = ServiceLevel.values()[random.nextInt(ServiceLevel.values().length)];
        int itemCount = random.nextInt(1, Math.min(4, products.size()) + 1);
        List<Product> shuffled = new ArrayList<>(products);
        java.util.Collections.shuffle(shuffled, random);
        List<OrderItem> items = shuffled.subList(0, itemCount).stream()
                .map(product -> new OrderItem(product, random.nextInt(1, 26)))
                .toList();
        return new Order(null, client, level, items, clock.getVirtualTimeMillis());
    }
}
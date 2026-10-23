package elmer.proyectofinal_sopes2.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;


public class Order {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final String id;
    private final Client client;
    private final ServiceLevel serviceLevel;
    private final List<OrderItem> items;
    private final CommodityType tipoMercanciaPrincipal;
    private final int cantidadTotalUnidades;

    
    private final long tiempoLlegada;
    private volatile long tiempoInicioAtencion = -1;
    private volatile long tiempoFinalizacion = -1;

    // Estado y prioridad
    private volatile OrderStatus estado = OrderStatus.CREADO;
    private volatile double prioridadEfectiva;

    public static final long MS_POR_UNIDAD_TRABAJO = 100L;

    public Order(String id, Client client, ServiceLevel serviceLevel, List<OrderItem> items, long tiempoLlegada) {
        this.id = (id != null && !id.isBlank()) ? id : generateId();
        this.client = Objects.requireNonNull(client, "El cliente no puede ser nulo");
        this.serviceLevel = Objects.requireNonNull(serviceLevel, "El nivel de servicio no puede ser nulo");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe contener al menos un producto");
        }
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.tiempoLlegada = tiempoLlegada;


        int sumaUnidades = 0;
        CommodityType tipoDominante = items.getFirst().getCommodityType();
        int maxRequerimientos = 0;

        for (OrderItem item : items) {
            sumaUnidades += item.getCantidad();
            // La mercancía más restrictiva (con más recursos solicitados) domina el pedido
            int numRecursos = item.getCommodityType().getRequiredResources().size();
            if (numRecursos > maxRequerimientos) {
                maxRequerimientos = numRecursos;
                tipoDominante = item.getCommodityType();
            }
        }

        this.cantidadTotalUnidades = sumaUnidades;
        this.tipoMercanciaPrincipal = tipoDominante;
        this.prioridadEfectiva = serviceLevel.getBasePriority();
    }

    public Order(Client client, ServiceLevel serviceLevel, List<OrderItem> items, long tiempoLlegada) {
        this(null, client, serviceLevel, items, tiempoLlegada);
    }

    public static String generateId() {
        return String.format("ORD-%04d", ID_GENERATOR.getAndIncrement());
    }

    public String getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public CommodityType getTipoMercanciaPrincipal() {
        return tipoMercanciaPrincipal;
    }

    public int getCantidadTotalUnidades() {
        return cantidadTotalUnidades;
    }

    public long getTiempoLlegada() {
        return tiempoLlegada;
    }

    public long getTiempoInicioAtencion() {
        return tiempoInicioAtencion;
    }

    public void setTiempoInicioAtencion(long tiempoInicioAtencion) {
        this.tiempoInicioAtencion = tiempoInicioAtencion;
    }

    public long getTiempoFinalizacion() {
        return tiempoFinalizacion;
    }

    public void setTiempoFinalizacion(long tiempoFinalizacion) {
        this.tiempoFinalizacion = tiempoFinalizacion;
    }

    public OrderStatus getEstado() {
        return estado;
    }

    public void setEstado(OrderStatus estado) {
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser nulo");
    }

    public double getPrioridadEfectiva() {
        return prioridadEfectiva;
    }

    public void setPrioridadEfectiva(double prioridadEfectiva) {
        this.prioridadEfectiva = prioridadEfectiva;
    }


    public long getTiempoTrabajoEstimadoMs() {
        return (long) cantidadTotalUnidades * MS_POR_UNIDAD_TRABAJO;
    }

 
    public long getTiempoEspera(long currentTimeMillis) {
        if (tiempoInicioAtencion > 0) {
            return tiempoInicioAtencion - tiempoLlegada;
        }
        return Math.max(0, currentTimeMillis - tiempoLlegada);
    }

 
    public long getTiempoRetorno() {
        if (tiempoFinalizacion > 0) {
            return tiempoFinalizacion - tiempoLlegada;
        }
        return -1;
    }


    public boolean haExcedidoSla(long currentTimeMillis, long msPorMinutoSimulado) {
        long esperaMinutos = getTiempoEspera(currentTimeMillis) / Math.max(1, msPorMinutoSimulado);
        return esperaMinutos > serviceLevel.getTargetWaitMinutes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %d unids | %s",
                id, client.getNombre(), serviceLevel.getDisplayName(),
                tipoMercanciaPrincipal.getDisplayName(), cantidadTotalUnidades, estado.getDisplayName());
    }
}

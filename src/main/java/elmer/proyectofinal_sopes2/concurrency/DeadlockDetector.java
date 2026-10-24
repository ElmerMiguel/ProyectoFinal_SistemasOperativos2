package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Inspecciona el grafo y publica incidentes cuando encuentra un ciclo. */
public final class DeadlockDetector {

    private final WaitForGraph graph;
    private final CopyOnWriteArrayList<Consumer<DeadlockIncident>> listeners = new CopyOnWriteArrayList<>();

    public DeadlockDetector(WaitForGraph graph) {
        this.graph = Objects.requireNonNull(graph, "El grafo no puede ser nulo");
    }

    /** Registra el escenario y marca las operaciones cuando aparece el ciclo. */
    public DeadlockIncident detect(Order operationA, ResourceType heldByA, ResourceType requestedByA,
                                   Order operationB, ResourceType heldByB, ResourceType requestedByB) {
        graph.addAssignment(heldByA.name(), operationA.getId());
        graph.addAssignment(heldByB.name(), operationB.getId());
        graph.addRequest(operationA.getId(), requestedByA.name());
        graph.addRequest(operationB.getId(), requestedByB.name());
        if (graph.findCycle().isEmpty()) {
            return null;
        }
        operationA.setEstado(OrderStatus.BLOQUEADO_DEADLOCK);
        operationB.setEstado(OrderStatus.BLOQUEADO_DEADLOCK);
        DeadlockIncident incident = new DeadlockIncident(operationA, heldByA, requestedByA,
                operationB, heldByB, requestedByB, System.currentTimeMillis(),
                "Espera circular: cada operacion retiene un recurso y solicita el recurso retenido por la otra.");
        listeners.forEach(listener -> listener.accept(incident));
        return incident;
    }

    public void addListener(Consumer<DeadlockIncident> listener) {
        listeners.add(Objects.requireNonNull(listener, "El listener no puede ser nulo"));
    }

    public WaitForGraph getGraph() {
        return graph;
    }
}
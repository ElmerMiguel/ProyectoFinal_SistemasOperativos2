package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/** Provoca el ciclo didactico con escaner, dos areas de carga y dos hilos reales. */
public final class DeadlockScenario {

    private final ResourceManager resourceManager;
    private final DeadlockDetector detector;
    private ResourceAllocation allocationA;
    private ResourceAllocation allocationB;
    private Thread threadA;
    private Thread threadB;

    public DeadlockScenario(ResourceManager resourceManager, DeadlockDetector detector) {
        this.resourceManager = Objects.requireNonNull(resourceManager, "El gestor no puede ser nulo");
        this.detector = Objects.requireNonNull(detector, "El detector no puede ser nulo");
    }

    /** Crea dos hilos que retienen recursos en orden inverso y retorna el incidente detectado. */
    public DeadlockIncident triggerDeliberateDeadlock(Order operationA, Order operationB)
            throws InterruptedException {
        CountDownLatch retained = new CountDownLatch(2);
        threadA = new Thread(() -> acquireA(operationA, retained), "deadlock-operation-a");
        threadB = new Thread(() -> acquireB(operationB, retained), "deadlock-operation-b");
        threadA.start();
        threadB.start();
        retained.await();
        return detector.detect(operationA, ResourceType.SISTEMA_ESCANEO, ResourceType.AREA_CARGA,
                operationB, ResourceType.AREA_CARGA, ResourceType.SISTEMA_ESCANEO);
    }

    /** Libera la asignacion de la victima, interrumpe su espera y limpia el grafo. */
    public void resolve(DeadlockIncident incident, Order chosenToContinue) {
        Objects.requireNonNull(incident, "El incidente no puede ser nulo");
        Objects.requireNonNull(chosenToContinue, "La operacion elegida es obligatoria");
        if (chosenToContinue.equals(incident.operationA())) {
            releaseVictim(incident.operationB(), allocationB, threadB);
            incident.operationA().setEstado(OrderStatus.EN_ESPERA_RECURSOS);
        } else if (chosenToContinue.equals(incident.operationB())) {
            releaseVictim(incident.operationA(), allocationA, threadA);
            incident.operationB().setEstado(OrderStatus.EN_ESPERA_RECURSOS);
        } else {
            throw new IllegalArgumentException("La operacion no pertenece al incidente");
        }
        clear(incident);
    }

    private void acquireA(Order order, CountDownLatch retained) {
        try {
            allocationA = resourceManager.acquireResource(order, ResourceType.SISTEMA_ESCANEO, 5_000);
            retained.countDown();
            retained.await();
            resourceManager.acquireResource(order, ResourceType.AREA_CARGA, 60_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void acquireB(Order order, CountDownLatch retained) {
        try {
            ResourceAllocation firstLoad = resourceManager.acquireResource(order, ResourceType.AREA_CARGA, 5_000);
            ResourceAllocation secondLoad = resourceManager.acquireResource(order, ResourceType.AREA_CARGA, 5_000);
            allocationB = merge(firstLoad, secondLoad);
            retained.countDown();
            retained.await();
            resourceManager.acquireResource(order, ResourceType.SISTEMA_ESCANEO, 60_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseVictim(Order order, ResourceAllocation allocation, Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
        if (allocation != null) {
            resourceManager.releaseResources(allocation);
        }
        order.setEstado(OrderStatus.EN_ESPERA_RECURSOS);
    }

    private void clear(DeadlockIncident incident) {
        WaitForGraph graph = detector.getGraph();
        graph.removeAssignment(incident.heldByA().name(), incident.operationA().getId());
        graph.removeAssignment(incident.heldByB().name(), incident.operationB().getId());
        graph.removeRequest(incident.operationA().getId(), incident.requestedByA().name());
        graph.removeRequest(incident.operationB().getId(), incident.requestedByB().name());
    }

    private static ResourceAllocation merge(ResourceAllocation first, ResourceAllocation second) {
        if (first == null || second == null) {
            throw new IllegalStateException("No se pudieron retener los recursos del escenario");
        }
        java.util.EnumMap<ResourceType, java.util.List<ResourceInstance>> values =
                new java.util.EnumMap<>(ResourceType.class);
        values.putAll(first.getInstances());
        second.getInstances().forEach((type, instances) -> values.merge(type, instances, (left, right) -> {
            java.util.ArrayList<ResourceInstance> merged = new java.util.ArrayList<>(left);
            merged.addAll(right);
            return java.util.List.copyOf(merged);
        }));
        return new ResourceAllocation(first.getOrder(), values);
    }
}
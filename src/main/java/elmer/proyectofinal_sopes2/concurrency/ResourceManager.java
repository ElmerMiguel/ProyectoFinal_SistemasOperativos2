package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Administra permisos y propietarios de los seis recursos finitos.
 */
public final class ResourceManager {

    private final EnumMap<ResourceType, Semaphore> semaphores = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, List<ResourceInstance>> instances = new EnumMap<>(ResourceType.class);
    private final ReentrantLock stateLock = new ReentrantLock();
    private final CopyOnWriteArrayList<ResourceListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Construye todas las instancias usando las capacidades definidas por el modelo.
     */
    public ResourceManager() {
        for (ResourceType type : ResourceType.values()) {
            semaphores.put(type, new Semaphore(type.getDefaultCapacity(), true));
            List<ResourceInstance> resourceInstances = new ArrayList<>();
            for (int index = 1; index <= type.getDefaultCapacity(); index++) {
                resourceInstances.add(new ResourceInstance(type.name() + "_" + index, type));
            }
            instances.put(type, Collections.unmodifiableList(resourceInstances));
        }
    }

    /**
     * Intenta reservar todos los recursos solicitados dentro del plazo indicado.
     * La reserva parcial se revierte en orden inverso ante timeout o interrupcion.
     *
     * @param order Pedido que sera propietario de la reserva.
     * @param needed Cantidad requerida por tipo de recurso.
     * @param timeoutMs Tiempo maximo total de espera en milisegundos.
     * @return Asignacion completa, o null si vencio el plazo.
     * @throws InterruptedException si el hilo es interrumpido durante la espera.
     */
    public ResourceAllocation acquireResources(Order order, Map<ResourceType, Integer> needed, long timeoutMs)
            throws InterruptedException {
        Objects.requireNonNull(order, "El pedido no puede ser nulo");
        validateRequirements(needed, timeoutMs);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        EnumMap<ResourceType, Integer> normalized = normalizeRequirements(needed);
        List<ResourceType> acquiredTypes = new ArrayList<>();
        EnumMap<ResourceType, List<ResourceInstance>> acquiredInstances = new EnumMap<>(ResourceType.class);

        try {
            for (ResourceType type : ResourceType.values()) {
                int amount = normalized.getOrDefault(type, 0);
                for (int index = 0; index < amount; index++) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0 || !semaphores.get(type).tryAcquire(remaining, TimeUnit.NANOSECONDS)) {
                        rollback(order, acquiredTypes, acquiredInstances);
                        return null;
                    }
                    acquiredTypes.add(type);
                }
            }

            stateLock.lockInterruptibly();
            try {
                long startTime = System.currentTimeMillis();
                for (ResourceType type : ResourceType.values()) {
                    int amount = normalized.getOrDefault(type, 0);
                    if (amount == 0) {
                        continue;
                    }
                    List<ResourceInstance> selected = instances.get(type).stream()
                            .filter(instance -> !instance.isInUse())
                            .limit(amount)
                            .toList();
                    if (selected.size() != amount) {
                        throw new IllegalStateException("El estado del semaforo no coincide con " + type);
                    }
                    selected.forEach(instance -> instance.assignTo(order, startTime));
                    acquiredInstances.put(type, selected);
                }
            } finally {
                stateLock.unlock();
            }

            ResourceAllocation allocation = new ResourceAllocation(order, acquiredInstances);
            notifyListeners(allocation, true);
            return allocation;
        } catch (InterruptedException exception) {
            rollback(order, acquiredTypes, acquiredInstances);
            Thread.currentThread().interrupt();
            throw exception;
        } catch (RuntimeException exception) {
            rollback(order, acquiredTypes, acquiredInstances);
            throw exception;
        }
    }

    /**
     * Libera una asignacion y devuelve todos sus permisos al semaforo correspondiente.
     *
     * @param allocation Reserva previamente retornada por este gestor.
     */
    public void releaseResources(ResourceAllocation allocation) {
        Objects.requireNonNull(allocation, "La asignacion no puede ser nula");
        stateLock.lock();
        try {
            allocation.getInstances().forEach((type, values) -> {
                values.forEach(instance -> instance.releaseFrom(allocation.getOrder()));
                semaphores.get(type).release(values.size());
            });
        } finally {
            stateLock.unlock();
        }
        notifyListeners(allocation, false);
    }

    /**
     * @param type Tipo de recurso consultado.
     * @return Cantidad de permisos libres.
     */
    public int getAvailableCount(ResourceType type) {
        return semaphore(type).availablePermits();
    }

    /**
     * @param type Tipo de recurso consultado.
     * @return Cantidad de instancias ocupadas.
     */
    public int getInUseCount(ResourceType type) {
        return type.getDefaultCapacity() - getAvailableCount(type);
    }

    /**
     * @param type Tipo de recurso consultado.
     * @return Snapshot inmutable de las instancias del tipo solicitado.
     */
    public List<ResourceInstance> getInstances(ResourceType type) {
        Objects.requireNonNull(type, "El tipo de recurso no puede ser nulo");
        stateLock.lock();
        try {
            return List.copyOf(instances.get(type));
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * @param type Tipo de recurso consultado.
     * @return Porcentaje de ocupacion entre cero y cien.
     */
    public double getUtilizationPercentage(ResourceType type) {
        return getInUseCount(type) * 100.0 / type.getDefaultCapacity();
    }

    /**
     * Registra un listener que recibira eventos de adquisicion y liberacion.
     *
     * @param listener Listener a registrar.
     */
    public void addListener(ResourceListener listener) {
        listeners.add(Objects.requireNonNull(listener, "El listener no puede ser nulo"));
    }

    /**
     * @param listener Listener que dejara de recibir eventos.
     */
    public void removeListener(ResourceListener listener) {
        listeners.remove(listener);
    }

    private void rollback(Order order, List<ResourceType> acquiredTypes,
                          Map<ResourceType, List<ResourceInstance>> acquiredInstances) {
        EnumMap<ResourceType, Integer> assignedCounts = new EnumMap<>(ResourceType.class);
        acquiredInstances.forEach((type, values) -> assignedCounts.put(type, values.size()));

        if (!acquiredInstances.isEmpty()) {
            stateLock.lock();
            try {
                acquiredInstances.forEach((type, values) -> {
                    values.forEach(instance -> instance.releaseFrom(order));
                });
            } finally {
                stateLock.unlock();
            }
        }

        for (int index = acquiredTypes.size() - 1; index >= 0; index--) {
            ResourceType type = acquiredTypes.get(index);
            int assigned = assignedCounts.getOrDefault(type, 0);
            if (assigned > 0) {
                assignedCounts.put(type, assigned - 1);
            } else {
                semaphores.get(type).release();
            }
        }

        assignedCounts.forEach((type, count) -> semaphores.get(type).release(count));
    }

    private void notifyListeners(ResourceAllocation allocation, boolean acquired) {
        listeners.forEach(listener -> {
            try {
                listener.onResourceAllocationChanged(allocation, acquired);
            } catch (RuntimeException ignored) {
                // Un listener de UI no debe alterar la reserva del recurso.
            }
        });
    }

    private Semaphore semaphore(ResourceType type) {
        return semaphores.get(Objects.requireNonNull(type, "El tipo de recurso no puede ser nulo"));
    }

    private static EnumMap<ResourceType, Integer> normalizeRequirements(Map<ResourceType, Integer> needed) {
        EnumMap<ResourceType, Integer> normalized = new EnumMap<>(ResourceType.class);
        needed.forEach((type, amount) -> normalized.merge(type, amount, Integer::sum));
        return normalized;
    }

    private static void validateRequirements(Map<ResourceType, Integer> needed, long timeoutMs) {
        Objects.requireNonNull(needed, "Los recursos requeridos no pueden ser nulos");
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("El timeout no puede ser negativo");
        }
        needed.forEach((type, amount) -> {
            Objects.requireNonNull(type, "El tipo de recurso no puede ser nulo");
            if (amount == null || amount < 0 || amount > type.getDefaultCapacity()) {
                throw new IllegalArgumentException("Cantidad invalida para " + type);
            }
        });
    }
}
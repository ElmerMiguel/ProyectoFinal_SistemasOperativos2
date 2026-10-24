package elmer.proyectofinal_sopes2.simulation;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/** Recolector thread-safe de contadores, tiempos y cumplimiento de SLA. */
public final class MetricsCollector {

    private final ReentrantLock lock = new ReentrantLock();
    private final LongAdder generated = new LongAdder();
    private final LongAdder completed = new LongAdder();
    private final LongAdder cancelled = new LongAdder();
    private final LongAdder totalWait = new LongAdder();
    private final LongAdder totalProcessing = new LongAdder();
    private final LongAdder slaMet = new LongAdder();
    private final EnumMap<ServiceLevel, LongAdder> waitsByLevel = new EnumMap<>(ServiceLevel.class);
    private final EnumMap<SchedulingPolicy, LongAdder> completedByPolicy = new EnumMap<>(SchedulingPolicy.class);
    private final EnumMap<ResourceType, LongAdder> utilizationSamples = new EnumMap<>(ResourceType.class);
    private long utilizationSampleCount;

    public MetricsCollector() {
        for (ServiceLevel level : ServiceLevel.values()) {
            waitsByLevel.put(level, new LongAdder());
        }
        for (SchedulingPolicy policy : SchedulingPolicy.values()) {
            completedByPolicy.put(policy, new LongAdder());
        }
        for (ResourceType type : ResourceType.values()) {
            utilizationSamples.put(type, new LongAdder());
        }
    }

    public void recordGenerated() {
        generated.increment();
    }

    public void recordCompleted(Order order, SchedulingPolicy policy) {
        completed.increment();
        long wait = order.getTiempoEspera(order.getTiempoInicioAtencion());
        long processing = Math.max(0, order.getTiempoFinalizacion() - order.getTiempoInicioAtencion());
        totalWait.add(wait);
        totalProcessing.add(processing);
        waitsByLevel.get(order.getServiceLevel()).add(wait);
        if (order.getTiempoEspera(order.getTiempoFinalizacion()) <= order.getServiceLevel().getTargetWaitMinutes() * 60_000L) {
            slaMet.increment();
        }
        completedByPolicy.get(policy).increment();
    }

    public void recordCancelled() {
        cancelled.increment();
    }

    public void recordResourceUtilization(Map<ResourceType, Double> utilization) {
        lock.lock();
        try {
            utilization.forEach((type, value) -> utilizationSamples.get(type).add(Math.round(value)));
            utilizationSampleCount++;
        } finally {
            lock.unlock();
        }
    }

    public void resetMetrics() {
        lock.lock();
        try {
            generated.reset();
            completed.reset();
            cancelled.reset();
            totalWait.reset();
            totalProcessing.reset();
            slaMet.reset();
            waitsByLevel.values().forEach(LongAdder::reset);
            completedByPolicy.values().forEach(LongAdder::reset);
            utilizationSamples.values().forEach(LongAdder::reset);
            utilizationSampleCount = 0;
        } finally {
            lock.unlock();
        }
    }

    public Snapshot snapshot() {
        lock.lock();
        try {
            EnumMap<ServiceLevel, Long> waits = new EnumMap<>(ServiceLevel.class);
            waitsByLevel.forEach((level, value) -> waits.put(level, value.sum()));
            EnumMap<SchedulingPolicy, Long> policies = new EnumMap<>(SchedulingPolicy.class);
            completedByPolicy.forEach((policy, value) -> policies.put(policy, value.sum()));
            EnumMap<ResourceType, Double> resources = new EnumMap<>(ResourceType.class);
            utilizationSamples.forEach((type, value) -> resources.put(type,
                    utilizationSampleCount == 0 ? 0 : value.sum() / (double) utilizationSampleCount));
            long completedCount = completed.sum();
            return new Snapshot(generated.sum(), completedCount, cancelled.sum(),
                    completedCount == 0 ? 0 : totalWait.sum() / (double) completedCount,
                    completedCount == 0 ? 0 : totalProcessing.sum() / (double) completedCount,
                    completedCount == 0 ? 0 : slaMet.sum() * 100.0 / completedCount,
                    waits, policies, resources);
        } finally {
            lock.unlock();
        }
    }

    public record Snapshot(long generated, long completed, long cancelled, double averageWaitMillis,
                           double averageProcessingMillis, double slaCompliancePercent,
                           Map<ServiceLevel, Long> waitsByLevel, Map<SchedulingPolicy, Long> completedByPolicy,
                           Map<ResourceType, Double> averageResourceUtilization) {
    }
}
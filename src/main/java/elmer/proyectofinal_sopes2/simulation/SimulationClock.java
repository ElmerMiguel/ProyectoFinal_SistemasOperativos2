package elmer.proyectofinal_sopes2.simulation;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Reloj virtual con pausa y multiplicadores de velocidad. */
public final class SimulationClock {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition resumed = lock.newCondition();
    private long realStartNanos = System.nanoTime();
    private long virtualAtStart;
    private double speedMultiplier = 1.0;
    private boolean paused;

    public long getVirtualTimeMillis() {
        lock.lock();
        try {
            return virtualAtStart + elapsedVirtualMillis();
        } finally {
            lock.unlock();
        }
    }

    public void setSpeedMultiplier(double multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("La velocidad no puede ser negativa");
        }
        lock.lock();
        try {
            checkpoint();
            speedMultiplier = multiplier;
            resumed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        try {
            checkpoint();
            paused = true;
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            checkpoint();
            paused = false;
            resumed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Espera tiempo real equivalente al intervalo virtual solicitado. */
    public void sleepVirtual(long virtualMillis) throws InterruptedException {
        if (virtualMillis < 0) {
            throw new IllegalArgumentException("El tiempo virtual no puede ser negativo");
        }
        long target = getVirtualTimeMillis() + virtualMillis;
        lock.lockInterruptibly();
        try {
            while (getVirtualTimeMillisLocked() < target) {
                while (paused || speedMultiplier == 0) {
                    resumed.await();
                }
                long remainingVirtual = target - getVirtualTimeMillisLocked();
                long realWait = Math.max(1L, (long) Math.ceil(remainingVirtual / speedMultiplier));
                resumed.awaitNanos(realWait * 1_000_000L);
            }
        } finally {
            lock.unlock();
        }
    }

    private void checkpoint() {
        virtualAtStart += elapsedVirtualMillis();
        realStartNanos = System.nanoTime();
    }

    private long elapsedVirtualMillis() {
        if (paused) {
            return 0L;
        }
        return (long) ((System.nanoTime() - realStartNanos) / 1_000_000.0 * speedMultiplier);
    }

    private long getVirtualTimeMillisLocked() {
        return virtualAtStart + elapsedVirtualMillis();
    }
}
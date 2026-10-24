package elmer.proyectofinal_sopes2.ipc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Buffer acotado FIFO entre Recepcion e Inventario.
 */
public final class ReceptionBuffer {

    public static final int DEFAULT_CAPACITY = 15;

    private final BlockingQueue<StockArrivalBatch> queue;

  
    public ReceptionBuffer() {
        this(DEFAULT_CAPACITY);
    }

    
    public ReceptionBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        queue = new ArrayBlockingQueue<>(capacity, true);
    }

   
    public void put(StockArrivalBatch batch) throws InterruptedException {
        queue.put(batch);
    }

   
    public StockArrivalBatch take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }
}
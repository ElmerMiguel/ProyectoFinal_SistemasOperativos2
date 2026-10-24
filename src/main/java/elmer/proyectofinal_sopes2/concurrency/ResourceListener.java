package elmer.proyectofinal_sopes2.concurrency;

/**
 * Recibe cambios de asignacion para actualizar monitores sin bloquear el gestor.
 */
@FunctionalInterface
public interface ResourceListener {

    /**
     * @param allocation Asignacion afectada.
     * @param acquired true si se adquirio; false si se libero.
     */
    void onResourceAllocationChanged(ResourceAllocation allocation, boolean acquired);
}
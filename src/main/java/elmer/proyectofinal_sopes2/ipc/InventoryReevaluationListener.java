package elmer.proyectofinal_sopes2.ipc;

import elmer.proyectofinal_sopes2.model.Product;

/**
 * Notifica al planificador que un producto ya puede reevaluarse.
 */
@FunctionalInterface
public interface InventoryReevaluationListener {

    void onInventoryUpdated(Product product, int receivedUnits);
}
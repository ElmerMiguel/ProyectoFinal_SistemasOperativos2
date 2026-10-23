package elmer.proyectofinal_sopes2.model;

import java.util.Objects;


public class StorageSlot {

    private final int pasillo;
    private final int estante;
    private final int nivel;
    private final int casilla;
    private final int capacidadMaxima;

    private int unidadesOcupadas;
    private Product productoActual;
    private String loteId;

    public StorageSlot(int pasillo, int estante, int nivel, int casilla, int capacidadMaxima) {
        this.pasillo = pasillo;
        this.estante = estante;
        this.nivel = nivel;
        this.casilla = casilla;
        this.capacidadMaxima = Math.max(1, capacidadMaxima);
        this.unidadesOcupadas = 0;
        this.productoActual = null;
        this.loteId = null;
    }

    public synchronized boolean isAvailable() {
        return unidadesOcupadas < capacidadMaxima;
    }

    public synchronized boolean isEmpty() {
        return unidadesOcupadas == 0;
    }

    public synchronized boolean isFull() {
        return unidadesOcupadas >= capacidadMaxima;
    }

    public synchronized int getEspacioDisponible() {
        return capacidadMaxima - unidadesOcupadas;
    }

    /**
     * Intenta almacenar unidades de un producto y lote en esta casilla fisica.
     * 
     * @param product Producto a guardar
     * @param loteId Identificador del lote
     * @param unidades Cantidad solicitada a guardar
     * @return Cantidad efectiva de unidades que pudieron almacenarse en esta casilla.
     */
    public synchronized int allocate(Product product, String loteId, int unidades) {
        Objects.requireNonNull(product, "El producto no puede ser nulo");
        if (unidades <= 0) {
            return 0;
        }

        if (productoActual != null && !productoActual.equals(product)) {
            return 0;
        }

        int espacio = getEspacioDisponible();
        if (espacio <= 0) {
            return 0;
        }

        int almacenar = Math.min(espacio, unidades);
        this.productoActual = product;
        this.loteId = loteId;
        this.unidadesOcupadas += almacenar;
        return almacenar;
    }

    /**
     * Libera o extrae unidades de esta casilla fisica para completar un pedido.
     * 
     * @param unidades Cantidad de unidades a extraer
     * @return Cantidad de unidades efectivamente extraídas.
     */
    public synchronized int release(int unidades) {
        if (unidades <= 0 || unidadesOcupadas == 0) {
            return 0;
        }

        int extraidas = Math.min(unidadesOcupadas, unidades);
        this.unidadesOcupadas -= extraidas;

        if (this.unidadesOcupadas == 0) {
            this.productoActual = null;
            this.loteId = null;
        }
        return extraidas;
    }

    public int getPasillo() {
        return pasillo;
    }

    public int getEstante() {
        return estante;
    }

    public int getNivel() {
        return nivel;
    }

    public int getCasilla() {
        return casilla;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public synchronized int getUnidadesOcupadas() {
        return unidadesOcupadas;
    }

    public synchronized Product getProductoActual() {
        return productoActual;
    }

    public synchronized String getLoteId() {
        return loteId;
    }

    public String getUbicacionCodigo() {
        return String.format("P%d-E%d-N%d-C%d", pasillo, estante, nivel, casilla);
    }

    @Override
    public synchronized String toString() {
        if (isEmpty()) {
            return String.format("[%s] Libre (0/%d)", getUbicacionCodigo(), capacidadMaxima);
        }
        return String.format("[%s] %s (%d/%d) Lote: %s",
                getUbicacionCodigo(), productoActual.getNombre(), unidadesOcupadas, capacidadMaxima, loteId);
    }
}

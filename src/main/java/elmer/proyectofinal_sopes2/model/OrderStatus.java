package elmer.proyectofinal_sopes2.model;


public enum OrderStatus {

    CREADO("Creado"),
    EN_ESPERA_INVENTARIO("En espera de inventario"),
    EN_ESPERA_RECURSOS("En espera de recursos"),
    EN_PROCESO("En proceso"),
    BLOQUEADO_DEADLOCK("Bloqueado en Deadlock"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

package elmer.proyectofinal_sopes2.scheduler;

/** Politicas disponibles para ordenar pedidos pendientes. */
public enum SchedulingPolicy {
    PRIORITY_STRICT("Prioridad Estricta por Nivel de Servicio"),
    AGING("Prioridad con Envejecimiento"),
    SHORTEST_JOB_FIRST("Menor Cantidad de Unidades Primero"),
    FIFO("Primero en Llegar, Primero en Atender");

    private final String displayName;

    SchedulingPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
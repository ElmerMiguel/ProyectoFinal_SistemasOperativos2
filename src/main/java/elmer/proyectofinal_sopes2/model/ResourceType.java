package elmer.proyectofinal_sopes2.model;

/**
 * def tipo de recursos
 */
public enum ResourceType {

  
    ESTACION_EMPAQUE(6, "Estación de Empaque"),

  
    AREA_CARGA(2, "Área de Carga de Camiones"),

    ENCARGADO_BODEGA(10, "Encargado de Bodega"),

   
    MONTACARGAS(3, "Montacargas"),

  
    ESTACION_CALIDAD(4, "Estación de Control de Calidad"),

   
    SISTEMA_ESCANEO(1, "Sistema de Escaneo de Códigos");

    private final int defaultCapacity;
    private final String displayName;

    ResourceType(int defaultCapacity, String displayName) {
        this.defaultCapacity = defaultCapacity;
        this.displayName = displayName;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName + " (" + defaultCapacity + " unidades)";
    }
}

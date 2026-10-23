package elmer.proyectofinal_sopes2.model;

/**
 * rep niveles de servicio 
 */
public enum ServiceLevel {

   
    NIVEL_1_EXPRES(1, "Exprés", 1, 100),

    NIVEL_2_PRIORITARIO(2, "Prioritario", 10, 80),

    
    NIVEL_3_ESTANDAR(3, "Estándar", 30, 60),

    NIVEL_4_PROGRAMADO(4, "Programado", 60, 40),

    
    NIVEL_5_ECONOMICO(5, "Económico", 120, 20);

    private final int levelNumber;
    private final String displayName;
    private final int targetWaitMinutes;
    private final int basePriority;

    ServiceLevel(int levelNumber, String displayName, int targetWaitMinutes, int basePriority) {
        this.levelNumber = levelNumber;
        this.displayName = displayName;
        this.targetWaitMinutes = targetWaitMinutes;
        this.basePriority = basePriority;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTargetWaitMinutes() {
        return targetWaitMinutes;
    }

    public int getBasePriority() {
        return basePriority;
    }

    @Override
    public String toString() {
        return "Nivel " + levelNumber + " - " + displayName + " (SLA: " + targetWaitMinutes + " min)";
    }
}

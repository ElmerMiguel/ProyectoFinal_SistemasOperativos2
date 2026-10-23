package elmer.proyectofinal_sopes2.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * tipos de mercancía manejados.
 */
public enum CommodityType {

    FRAGIL(
            "Frágil",
            "Artículos delicados que requieren inspección exhaustiva y empaque con material de amortiguación.",
            createResourceRequirements(Map.of(
                    ResourceType.ESTACION_CALIDAD, 1,
                    ResourceType.ENCARGADO_BODEGA, 1,
                    ResourceType.ESTACION_EMPAQUE, 1
            ))
    ),


    PESADO(
            "Pesado",
            "Carga pesada o sobredimensionada que requiere transporte mecánico y bahía de carga.",
            createResourceRequirements(Map.of(
                    ResourceType.MONTACARGAS, 1,
                    ResourceType.ENCARGADO_BODEGA, 2,
                    ResourceType.ESTACION_EMPAQUE, 1,
                    ResourceType.AREA_CARGA, 1
            ))
    ),


    ELECTRONICA_VALOR(
            "Electrónica de Alto Valor",
            "Dispositivos sensibles que exigen verificación serializada por escáner y sello de seguridad.",
            createResourceRequirements(Map.of(
                    ResourceType.SISTEMA_ESCANEO, 1,
                    ResourceType.ESTACION_CALIDAD, 1,
                    ResourceType.ENCARGADO_BODEGA, 1,
                    ResourceType.ESTACION_EMPAQUE, 1
            ))
    ),

  
    ESTANDAR(
            "Estándar",
            "Mercancía de consumo general con manipulación directa y embalaje simplificado.",
            createResourceRequirements(Map.of(
                    ResourceType.ENCARGADO_BODEGA, 1,
                    ResourceType.ESTACION_EMPAQUE, 1
            ))
    ),

    /**
     * Lotes mayoristas paletizados (alimentos no perecederos, suministros industriales).
     * Recursos: 1 Montacargas, 1 Sistema de Escaneo, 1 Área de Carga, 1 Encargado de Bodega.
     */
    BULK_PALETIZADO(
            "Carga Paletizada (Bulk)",
            "Lotes mayoristas estibados que requieren escaneo de manifiesto de pallet y camión de carga.",
            createResourceRequirements(Map.of(
                    ResourceType.MONTACARGAS, 1,
                    ResourceType.SISTEMA_ESCANEO, 1,
                    ResourceType.AREA_CARGA, 1,
                    ResourceType.ENCARGADO_BODEGA, 1
            ))
    );

    private final String displayName;
    private final String description;
    private final Map<ResourceType, Integer> requiredResources;

    CommodityType(String displayName, String description, Map<ResourceType, Integer> requiredResources) {
        this.displayName = displayName;
        this.description = description;
        this.requiredResources = requiredResources;
    }

    private static Map<ResourceType, Integer> createResourceRequirements(Map<ResourceType, Integer> map) {
        EnumMap<ResourceType, Integer> enumMap = new EnumMap<>(ResourceType.class);
        enumMap.putAll(map);
        return Collections.unmodifiableMap(enumMap);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Retorna el mapa inmutable con los tipos y cantidades de recursos requeridos.
     * @return Mapa de tipo de recurso a cantidad de instancias necesarias.
     */
    public Map<ResourceType, Integer> getRequiredResources() {
        return requiredResources;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

package elmer.proyectofinal_sopes2.ipc;

import elmer.proyectofinal_sopes2.model.CommodityType;
import elmer.proyectofinal_sopes2.model.Product;

import java.util.List;

/**
 * Catalogo inicial de demostracion con productos de los cinco tipos de mercancia.
 */
public final class DefaultProductCatalog {

    private DefaultProductCatalog() {
    }

    public static List<Product> create() {
        return List.of(
                new Product("PROD-01", "Vajilla de Cristal", CommodityType.FRAGIL, 20, 0),
                new Product("PROD-02", "Botellas de Laboratorio", CommodityType.FRAGIL, 20, 0),
                new Product("PROD-03", "Motor Electrico", CommodityType.PESADO, 20, 0),
                new Product("PROD-04", "Refrigerador Industrial", CommodityType.PESADO, 20, 0),
                new Product("PROD-05", "Smartphone Pro", CommodityType.ELECTRONICA_VALOR, 20, 0),
                new Product("PROD-06", "Laptop Empresarial", CommodityType.ELECTRONICA_VALOR, 20, 0),
                new Product("PROD-07", "Camisetas de Algodon", CommodityType.ESTANDAR, 20, 0),
                new Product("PROD-08", "Cuadernos Escolares", CommodityType.ESTANDAR, 20, 0),
                new Product("PROD-09", "Cemento en Palet", CommodityType.BULK_PALETIZADO, 20, 0),
                new Product("PROD-10", "Granos en Palet", CommodityType.BULK_PALETIZADO, 20, 0)
        );
    }
}
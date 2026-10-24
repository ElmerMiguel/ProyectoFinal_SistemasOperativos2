package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.concurrency.DeadlockDetector;
import elmer.proyectofinal_sopes2.concurrency.DeadlockIncident;
import elmer.proyectofinal_sopes2.concurrency.DeadlockScenario;
import elmer.proyectofinal_sopes2.concurrency.WaitForGraph;
import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import elmer.proyectofinal_sopes2.simulation.MetricsCollector;
import elmer.proyectofinal_sopes2.simulation.SimulationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integracion completa de extremo a extremo (End-to-End) para REDXela.
 * Valida la integracion armonica de todos los subsistemas de Sistemas Operativos 2:
 * 1. Concurrencia y 6 recursos compartidos finitos.
 * 2. IPC con buffer acotado entre Recepcion e Inventario.
 * 3. Almacenamiento no contiguo (Paginacion de memoria fisica).
 * 4. Planificacion multinivel de pedidos (Priority, Aging, SJF, FIFO).
 * 5. Deteccion y resolucion manual de Deadlock (Condiciones de Coffman).
 * 6. Colector de metricas y cumplimiento de SLAs.
 */
class REDXelaSystemIntegrationTest {

    @Test
    @DisplayName("Flujo completo de integracion: IPC -> Almacen -> Planificador -> Deadlock -> Metricas")
    void fullSystemIntegrationFlow() throws Exception {
        SimulationController controller = new SimulationController();

        try {
            // 1. Validar inicializacion de los 6 recursos finitos
            assertEquals(6, controller.getResourceManager().getAvailableCount(ResourceType.ESTACION_EMPAQUE));
            assertEquals(2, controller.getResourceManager().getAvailableCount(ResourceType.AREA_CARGA));
            assertEquals(10, controller.getResourceManager().getAvailableCount(ResourceType.ENCARGADO_BODEGA));
            assertEquals(3, controller.getResourceManager().getAvailableCount(ResourceType.MONTACARGAS));
            assertEquals(4, controller.getResourceManager().getAvailableCount(ResourceType.ESTACION_CALIDAD));
            assertEquals(1, controller.getResourceManager().getAvailableCount(ResourceType.SISTEMA_ESCANEO));

            // 2. Ingesta de stock mediante IPC (Recepcion -> Buffer Acotado)
            List<Product> products = controller.getProducts();
            assertFalse(products.isEmpty(), "El catalogo debe contener productos iniciales");
            Product fragilProd = products.getFirst();

            StockArrivalBatch batch = new StockArrivalBatch(
                    "LOT-INTEG-001", 100L, fragilProd, 80, "CAMION-INTEG-01");
            controller.receiveManualStock(batch);
            assertTrue(controller.getReceptionBuffer().size() >= 0);

            // 3. Iniciar motor de simulacion y workers
            controller.start();

            // Esperar que el worker de inventario procese el lote desde el buffer IPC
            boolean stockLoaded = false;
            for (int i = 0; i < 20; i++) {
                if (controller.getInventoryManager().getStock(fragilProd) >= 80) {
                    stockLoaded = true;
                    break;
                }
                Thread.sleep(100);
            }
            assertTrue(stockLoaded, "El worker de inventario debio procesar el lote del buffer IPC y poblar el stock");

            // 4. Validar asignacion fisica no contigua en el almacen (Paginacion)
            var occupiedSlots = controller.getInventoryManager().getWarehouse().getOccupiedSlots(fragilProd);
            assertFalse(occupiedSlots.isEmpty(), "Las casillas de almacen deben reflejar el lote almacenado");

            // 5. Crear y encolar pedidos de distintos niveles de servicio
            Client client = new Client("CLI-TEST-01", "Empresa Xela", ServiceLevel.NIVEL_1_EXPRES);
            Order expressOrder = new Order("ORD-EXP-01", client, ServiceLevel.NIVEL_1_EXPRES,
                    List.of(new OrderItem(fragilProd, 5)), 200L);
            Order standardOrder = new Order("ORD-STD-01", client, ServiceLevel.NIVEL_3_ESTANDAR,
                    List.of(new OrderItem(fragilProd, 10)), 205L);

            controller.createManualOrder(standardOrder);
            controller.createManualOrder(expressOrder);

            // 6. Validar cambio dinamico de politicas en el planificador
            controller.getScheduler().setPolicy(SchedulingPolicy.PRIORITY_STRICT);
            assertEquals(SchedulingPolicy.PRIORITY_STRICT, controller.getScheduler().getPolicy());

            controller.getScheduler().setPolicy(SchedulingPolicy.SHORTEST_JOB_FIRST);
            assertEquals(SchedulingPolicy.SHORTEST_JOB_FIRST, controller.getScheduler().getPolicy());

            controller.getScheduler().setPolicy(SchedulingPolicy.AGING);
            controller.getScheduler().ageOrders(5000);
            assertEquals(SchedulingPolicy.AGING, controller.getScheduler().getPolicy());

            // 7. Esperar que al menos un pedido sea procesado y completado por los OrderWorkers
            boolean orderCompleted = false;
            for (int i = 0; i < 40; i++) {
                if (controller.getMetrics().snapshot().completed() > 0) {
                    orderCompleted = true;
                    break;
                }
                Thread.sleep(150);
            }
            assertTrue(orderCompleted, "Al menos un pedido debio completarse en las estaciones de trabajo");

            // 8. Validar recoleccion de metricas y SLAs
            MetricsCollector.Snapshot snapshot = controller.getMetrics().snapshot();
            assertTrue(snapshot.completed() >= 1);
            assertTrue(snapshot.slaCompliancePercent() >= 0.0);

            // 9. Validar escenario deliberado de Deadlock y resolucion manual
            Order dlOrderA = new Order("DL-INT-A", client, ServiceLevel.NIVEL_1_EXPRES,
                    List.of(new OrderItem(fragilProd, 1)), 300L);
            Order dlOrderB = new Order("DL-INT-B", client, ServiceLevel.NIVEL_1_EXPRES,
                    List.of(new OrderItem(fragilProd, 1)), 301L);

            WaitForGraph graph = new WaitForGraph();
            DeadlockDetector detector = new DeadlockDetector(graph);
            DeadlockScenario scenario = new DeadlockScenario(controller.getResourceManager(), detector);

            DeadlockIncident incident = scenario.triggerDeliberateDeadlock(dlOrderA, dlOrderB);
            assertNotNull(incident, "El detector debe identificar el incidente de deadlock");
            assertEquals(OrderStatus.BLOQUEADO_DEADLOCK, dlOrderA.getEstado());
            assertEquals(OrderStatus.BLOQUEADO_DEADLOCK, dlOrderB.getEstado());

            // Resolver manualmente favoreciendo a la Operación A
            scenario.resolve(incident, dlOrderA);
            Thread.sleep(100);
            assertTrue(graph.findCycle().isEmpty(), "El ciclo en el grafo debe desaparecer tras la resolucion");
            assertEquals(OrderStatus.EN_ESPERA_RECURSOS, dlOrderB.getEstado(),
                    "La operacion victima debe haber liberado recursos y regresado a la cola de espera");

        } finally {
            // 10. Detencion limpia de todos los hilos
            controller.stop();
        }
    }
}

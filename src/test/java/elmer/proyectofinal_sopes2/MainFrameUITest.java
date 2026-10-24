package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.simulation.SimulationController;
import elmer.proyectofinal_sopes2.ui.MetricsPanel;
import elmer.proyectofinal_sopes2.ui.OrderTrackingPanel;
import elmer.proyectofinal_sopes2.ui.ReceptionPanel;
import elmer.proyectofinal_sopes2.ui.ResourceMonitorPanel;
import elmer.proyectofinal_sopes2.ui.WarehouseVisualizerPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainFrameUITest {

    @Test
    @DisplayName("Todos los paneles y componentes de la UI se instancian y refrescan limpiamente")
    void uiPanelsInitializeAndRefreshCleanly() throws Exception {
        SwingUtilities.invokeAndWait(() -> assertDoesNotThrow(() -> {
            SimulationController controller = new SimulationController();

            ResourceMonitorPanel resourcePanel = new ResourceMonitorPanel(controller.getResourceManager());
            assertNotNull(resourcePanel);
            resourcePanel.refresh();

            OrderTrackingPanel orderPanel = new OrderTrackingPanel(controller.getScheduler());
            assertNotNull(orderPanel);
            orderPanel.refresh();

            WarehouseVisualizerPanel warehousePanel = new WarehouseVisualizerPanel(controller.getInventoryManager().getWarehouse());
            assertNotNull(warehousePanel);
            warehousePanel.refresh();

            ReceptionPanel receptionPanel = new ReceptionPanel(controller.getReceptionBuffer(), controller.getInventoryManager());
            assertNotNull(receptionPanel);
            receptionPanel.refresh();

            MetricsPanel metricsPanel = new MetricsPanel(controller.getMetrics());
            assertNotNull(metricsPanel);
            metricsPanel.refresh();

            if (!GraphicsEnvironment.isHeadless()) {
                elmer.proyectofinal_sopes2.ui.MainFrame frame = new elmer.proyectofinal_sopes2.ui.MainFrame(controller);
                assertNotNull(frame.getTitle());
                frame.dispose();
            }
        }));
    }
}

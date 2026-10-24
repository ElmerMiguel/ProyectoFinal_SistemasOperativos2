package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderStatus;
import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.scheduler.OrderScheduler;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Presenta en tiempo real los pedidos en proceso activo y las colas de espera
 * (tanto los elegibles para recursos como los bloqueados por falta de stock).
 */
public final class OrderTrackingPanel extends JPanel {

    private final OrderScheduler scheduler;

    // Modelos de datos para las tablas
    private final DefaultTableModel activeModel;
    private final DefaultTableModel readyModel;
    private final DefaultTableModel inventoryModel;

    // Componentes de tabla
    private final JTable activeTable;
    private final JTable readyTable;
    private final JTable inventoryTable;

    private final JTabbedPane queueTabs;

    public OrderTrackingPanel(OrderScheduler scheduler) {
        super(new BorderLayout(8, 8));
        this.scheduler = scheduler;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Tabla de Pedidos en Proceso Activo
        String[] activeCols = {"ID Pedido", "Cliente", "Nivel de Servicio", "Mercancia", "Unidades",
                "Recursos Asignados", "Progreso", "Tiempo Restante"};
        activeModel = createModel(activeCols);
        activeTable = configureTable(activeModel);

        JPanel activePanel = new JPanel(new BorderLayout(4, 4));
        activePanel.setBorder(BorderFactory.createTitledBorder("Pedidos en Proceso Activo (Estaciones de Trabajo)"));
        activePanel.add(new JScrollPane(activeTable), BorderLayout.CENTER);

        // 2. Colas de Espera con Pestañas
        String[] readyCols = {"Pos.", "ID Pedido", "Cliente", "Nivel de Servicio", "Unidades",
                "Tiempo en Espera", "Prioridad Efectiva", "Recursos Requeridos"};
        readyModel = createModel(readyCols);
        readyTable = configureTable(readyModel);

        String[] invCols = {"Pos.", "ID Pedido", "Cliente", "Nivel de Servicio", "Unidades",
                "Tiempo en Espera", "Causa de Bloqueo"};
        inventoryModel = createModel(invCols);
        inventoryTable = configureTable(inventoryModel);

        queueTabs = new JTabbedPane();
        queueTabs.addTab("Esperando Recursos (0)", new JScrollPane(readyTable));
        queueTabs.addTab("Esperando Inventario / Stock (0)", new JScrollPane(inventoryTable));

        JPanel queuePanel = new JPanel(new BorderLayout(4, 4));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Colas de Planificacion y Espera"));
        queuePanel.add(queueTabs, BorderLayout.CENTER);

        // Divisor vertical armónico
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, activePanel, queuePanel);
        split.setResizeWeight(0.42);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /**
     * Actualiza todas las tablas en el EDT.
     */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Pedidos Activos
        activeModel.setRowCount(0);
        List<Order> activeOrders = scheduler.getActiveOrders();
        for (Order order : activeOrders) {
            long burstMs = order.getTiempoTrabajoEstimadoMs();
            long elapsedMs = (order.getTiempoInicioAtencion() > 0)
                    ? Math.max(0, now - order.getTiempoInicioAtencion()) : 0;
            int progressPercent = (burstMs > 0) ? (int) Math.min(100, (elapsedMs * 100) / burstMs) : 100;
            long remainingMs = Math.max(0, burstMs - elapsedMs);

            String resourcesStr = formatResources(order.getTipoMercanciaPrincipal().getRequiredResources());

            activeModel.addRow(new Object[]{
                    order.getId(),
                    order.getClient().getNombre(),
                    order.getServiceLevel().getDisplayName(),
                    order.getTipoMercanciaPrincipal().getDisplayName(),
                    order.getCantidadTotalUnidades(),
                    resourcesStr,
                    progressPercent + "%",
                    (remainingMs / 1000) + " s (" + remainingMs + " ms)"
            });
        }

        // 2. Cola Listos (Esperando Recursos)
        readyModel.setRowCount(0);
        List<Order> readyOrders = scheduler.getReadyOrders();
        int pos = 1;
        for (Order order : readyOrders) {
            long waitMs = order.getTiempoEspera(now);
            String waitStr = (waitMs / 1000) + " s";
            String resourcesStr = formatResources(order.getTipoMercanciaPrincipal().getRequiredResources());

            readyModel.addRow(new Object[]{
                    pos++,
                    order.getId(),
                    order.getClient().getNombre(),
                    order.getServiceLevel().getDisplayName(),
                    order.getCantidadTotalUnidades(),
                    waitStr,
                    String.format("%.2f", order.getPrioridadEfectiva()),
                    resourcesStr
            });
        }
        queueTabs.setTitleAt(0, "Esperando Recursos (" + readyOrders.size() + ")");

        // 3. Cola Esperando Inventario
        inventoryModel.setRowCount(0);
        List<Order> invOrders = scheduler.getWaitingForInventory();
        pos = 1;
        for (Order order : invOrders) {
            long waitMs = order.getTiempoEspera(now);
            String waitStr = (waitMs / 1000) + " s";

            inventoryModel.addRow(new Object[]{
                    pos++,
                    order.getId(),
                    order.getClient().getNombre(),
                    order.getServiceLevel().getDisplayName(),
                    order.getCantidadTotalUnidades(),
                    waitStr,
                    "Sin stock fisico en almacen"
            });
        }
        queueTabs.setTitleAt(1, "Esperando Inventario / Stock (" + invOrders.size() + ")");
    }

    private static String formatResources(Map<ResourceType, Integer> resources) {
        if (resources == null || resources.isEmpty()) return "Ninguno";
        return resources.entrySet().stream()
                .map(e -> e.getKey().getDisplayName() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable configureTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        if (table.getColumnCount() > 4) {
            table.getColumnModel().getColumn(4).setCellRenderer(center);
        }
        return table;
    }
}

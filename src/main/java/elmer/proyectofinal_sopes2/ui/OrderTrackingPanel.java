package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.scheduler.OrderScheduler;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Presenta pedidos elegibles y pedidos retenidos por falta de inventario. */
public final class OrderTrackingPanel extends JPanel {

    private final OrderScheduler scheduler;
    private final DefaultTableModel readyModel = model("Posicion", "ID", "Cliente", "Nivel", "Unidades", "Espera", "Prioridad");
    private final DefaultTableModel inventoryModel = model("Posicion", "ID", "Cliente", "Nivel", "Unidades", "Espera", "Causa");

    public OrderTrackingPanel(OrderScheduler scheduler) {
        super(new BorderLayout());
        this.scheduler = scheduler;
        JTable ready = new JTable(readyModel);
        JTable inventory = new JTable(inventoryModel);
        JPanel top = titled("Cola de recursos", ready);
        JPanel bottom = titled("Esperando inventario", inventory);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /** Actualiza las colas sin tocar componentes fuera del EDT. */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        readyModel.setRowCount(0);
        int position = 1;
        for (Order order : scheduler.getReadyOrders()) {
            readyModel.addRow(new Object[]{position++, order.getId(), order.getClient().getNombre(),
                    order.getServiceLevel().getDisplayName(), order.getCantidadTotalUnidades(),
                    order.getTiempoEspera(System.currentTimeMillis()), order.getPrioridadEfectiva()});
        }
        inventoryModel.setRowCount(0);
        position = 1;
        for (Order order : scheduler.getWaitingForInventory()) {
            inventoryModel.addRow(new Object[]{position++, order.getId(), order.getClient().getNombre(),
                    order.getServiceLevel().getDisplayName(), order.getCantidadTotalUnidades(),
                    order.getTiempoEspera(System.currentTimeMillis()), "Stock de inventario"});
        }
    }

    private static DefaultTableModel model(Object... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JPanel titled(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table));
        return panel;
    }
}
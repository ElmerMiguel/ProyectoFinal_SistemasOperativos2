package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import elmer.proyectofinal_sopes2.simulation.MetricsCollector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Panel de KPIs de pedidos, SLA y comparacion de politicas. */
public final class MetricsPanel extends JPanel {

    private final MetricsCollector metrics;
    private final JLabel summary = new JLabel();
    private final DefaultTableModel levelModel = model("Nivel", "Espera acumulada (ms)");
    private final DefaultTableModel policyModel = model("Politica", "Completados");
    private final DefaultTableModel resourceModel = model("Recurso", "Ocupacion promedio (%)");

    public MetricsPanel(MetricsCollector metrics) {
        super(new BorderLayout(8, 8));
        this.metrics = metrics;
        JPanel tables = new JPanel(new GridLayout(1, 3, 8, 8));
        tables.add(titled("Espera por nivel", new JTable(levelModel)));
        tables.add(titled("Politicas", new JTable(policyModel)));
        tables.add(titled("Recursos", new JTable(resourceModel)));
        add(summary, BorderLayout.NORTH);
        add(tables, BorderLayout.CENTER);
        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /** Actualiza los indicadores en el hilo grafico. */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        MetricsCollector.Snapshot snapshot = metrics.snapshot();
        summary.setText(String.format("Generados: %d | Completados: %d | Cancelados: %d | Espera media: %.1f ms | Proceso medio: %.1f ms | SLA: %.1f%%",
                snapshot.generated(), snapshot.completed(), snapshot.cancelled(), snapshot.averageWaitMillis(),
                snapshot.averageProcessingMillis(), snapshot.slaCompliancePercent()));
        levelModel.setRowCount(0);
        for (ServiceLevel level : ServiceLevel.values()) {
            levelModel.addRow(new Object[]{level.getDisplayName(), snapshot.waitsByLevel().get(level)});
        }
        policyModel.setRowCount(0);
        for (SchedulingPolicy policy : SchedulingPolicy.values()) {
            policyModel.addRow(new Object[]{policy.getDisplayName(), snapshot.completedByPolicy().get(policy)});
        }
        resourceModel.setRowCount(0);
        for (ResourceType type : ResourceType.values()) {
            resourceModel.addRow(new Object[]{type.getDisplayName(), snapshot.averageResourceUtilization().get(type)});
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
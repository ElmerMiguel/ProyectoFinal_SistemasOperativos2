package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.ResourceType;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import elmer.proyectofinal_sopes2.simulation.MetricsCollector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Panel de estadisticas ejecutivas, comparacion de politicas de planificacion y KPIs de SLAs.
 */
public final class MetricsPanel extends JPanel {

    private final MetricsCollector metrics;

    // Etiquetas de tarjetas KPI
    private final JLabel lblGenerated = new JLabel("0");
    private final JLabel lblCompleted = new JLabel("0");
    private final JLabel lblCancelled = new JLabel("0");
    private final JLabel lblAvgWait = new JLabel("0.0 ms");
    private final JLabel lblAvgProcess = new JLabel("0.0 ms");
    private final JLabel lblSlaCompliance = new JLabel("100.0%");

    // Modelos de tablas de desglose
    private final DefaultTableModel levelModel;
    private final DefaultTableModel policyModel;
    private final DefaultTableModel resourceModel;

    public MetricsPanel(MetricsCollector metrics) {
        super(new BorderLayout(8, 8));
        this.metrics = metrics;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Dashboard Superior: 6 tarjetas de KPIs
        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 8, 8));
        kpiGrid.setBorder(BorderFactory.createTitledBorder("Indicadores Clave de Desempeño (KPIs de Simulación)"));

        kpiGrid.add(createKpiCard("Pedidos Generados", lblGenerated));
        kpiGrid.add(createKpiCard("Completados", lblCompleted));
        kpiGrid.add(createKpiCard("Cancelados", lblCancelled));
        kpiGrid.add(createKpiCard("Espera Promedio", lblAvgWait));
        kpiGrid.add(createKpiCard("Proceso Promedio", lblAvgProcess));
        kpiGrid.add(createKpiCard("Cumplimiento SLA", lblSlaCompliance));
        add(kpiGrid, BorderLayout.NORTH);

        // 2. Tablas detalladas en pestañas organizadas
        JTabbedPane tabs = new JTabbedPane();

        String[] levelCols = {"Nivel de Servicio", "Tipo de Envio", "SLA Objetivo", "Espera Acumulada"};
        levelModel = createModel(levelCols);
        tabs.addTab("Tiempos de Espera por Nivel", new JScrollPane(configureTable(levelModel)));

        String[] policyCols = {"Politica de Atencion Evaluada", "Pedidos Completados Exitosamente"};
        policyModel = createModel(policyCols);
        tabs.addTab("Comparativa de Politicas de Planificacion", new JScrollPane(configureTable(policyModel)));

        String[] resCols = {"Recurso Compartido", "Capacidad Fisica", "Ocupacion Promedio"};
        resourceModel = createModel(resCols);
        tabs.addTab("Utilizacion de Recursos Finitos", new JScrollPane(configureTable(resourceModel)));

        add(tabs, BorderLayout.CENTER);

        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /**
     * Refresca los valores y snapshots en el EDT.
     */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        MetricsCollector.Snapshot snapshot = metrics.snapshot();
        lblGenerated.setText(String.valueOf(snapshot.generated()));
        lblCompleted.setText(String.valueOf(snapshot.completed()));
        lblCancelled.setText(String.valueOf(snapshot.cancelled()));
        lblAvgWait.setText(String.format("%.1f ms", snapshot.averageWaitMillis()));
        lblAvgProcess.setText(String.format("%.1f ms", snapshot.averageProcessingMillis()));
        lblSlaCompliance.setText(String.format("%.1f%%", snapshot.slaCompliancePercent()));

        // Tabla por nivel
        levelModel.setRowCount(0);
        for (ServiceLevel level : ServiceLevel.values()) {
            Long waitMs = snapshot.waitsByLevel().getOrDefault(level, 0L);
            levelModel.addRow(new Object[]{
                    "Nivel " + level.getLevelNumber(),
                    level.getDisplayName(),
                    level.getTargetWaitMinutes() + " min",
                    (waitMs / 1000) + " s (" + waitMs + " ms)"
            });
        }

        // Tabla de politicas
        policyModel.setRowCount(0);
        for (SchedulingPolicy policy : SchedulingPolicy.values()) {
            Long count = snapshot.completedByPolicy().getOrDefault(policy, 0L);
            policyModel.addRow(new Object[]{
                    policy.getDisplayName(),
                    count + " pedidos"
            });
        }

        // Tabla de recursos
        resourceModel.setRowCount(0);
        for (ResourceType type : ResourceType.values()) {
            Double util = snapshot.averageResourceUtilization().getOrDefault(type, 0.0);
            resourceModel.addRow(new Object[]{
                    type.getDisplayName(),
                    type.getDefaultCapacity() + " unidades",
                    String.format("%.1f%%", util)
            });
        }
    }

    private static JPanel createKpiCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 16f));
        valueLabel.setHorizontalAlignment(JLabel.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
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
        if (table.getColumnCount() >= 2) {
            table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellRenderer(center);
        }
        return table;
    }
}

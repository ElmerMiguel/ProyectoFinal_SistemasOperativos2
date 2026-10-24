package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.concurrency.ResourceInstance;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;

/**
 * Monitor visual de los 6 recursos compartidos y sus instancias fisicas.
 * Presenta un resumen ejecutivo superior con barras de uso y una tabla maestra
 * de instancias ordenadas y filtrables.
 */
public final class ResourceMonitorPanel extends JPanel {

    private final ResourceManager resourceManager;
    private final Map<ResourceType, JProgressBar> progressBars = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, JLabel> countLabels = new EnumMap<>(ResourceType.class);
    private final DefaultTableModel instanceModel;
    private final JTable instanceTable;
    private final JComboBox<String> filterCombo;
    private final JLabel globalSummaryLabel = new JLabel();

    public ResourceMonitorPanel(ResourceManager resourceManager) {
        super(new BorderLayout(8, 8));
        this.resourceManager = resourceManager;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Panel Superior: 6 tarjetas ejecutivas de recursos
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 10, 8));
        cardsPanel.setBorder(BorderFactory.createTitledBorder("Nivel de Ocupacion por Tipo de Recurso"));

        for (ResourceType type : ResourceType.values()) {
            JPanel card = new JPanel(new BorderLayout(6, 4));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            JLabel title = new JLabel(type.getDisplayName());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));

            JLabel countLabel = new JLabel(type.getDefaultCapacity() + " de " + type.getDefaultCapacity() + " libres");
            countLabels.put(type, countLabel);

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString("0% en uso");
            progressBar.setPreferredSize(new Dimension(160, 20));
            progressBars.put(type, progressBar);

            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.add(title, BorderLayout.NORTH);
            textPanel.add(countLabel, BorderLayout.SOUTH);

            card.add(textPanel, BorderLayout.NORTH);
            card.add(progressBar, BorderLayout.CENTER);
            cardsPanel.add(card);
        }

        // 2. Panel Inferior: Tabla maestra de todas las instancias
        JPanel tableContainer = new JPanel(new BorderLayout(4, 4));
        tableContainer.setBorder(BorderFactory.createTitledBorder("Detalle de Instancias Fisicas"));

        JPanel tableHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));
        tableHeader.add(new JLabel("Filtrar por tipo:"));
        filterCombo = new JComboBox<>();
        filterCombo.addItem("Todos los recursos (26)");
        for (ResourceType type : ResourceType.values()) {
            filterCombo.addItem(type.getDisplayName());
        }
        filterCombo.addActionListener(e -> refresh());
        tableHeader.add(filterCombo);
        tableHeader.add(globalSummaryLabel);
        tableContainer.add(tableHeader, BorderLayout.NORTH);

        String[] columns = {"ID Instancia", "Tipo de Recurso", "Estado", "Pedido Asignado", "Cliente", "Tiempo en Uso"};
        instanceModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        instanceTable = new JTable(instanceModel);
        instanceTable.setRowHeight(24);
        instanceTable.setFillsViewportHeight(true);
        instanceTable.getTableHeader().setReorderingAllowed(false);

        // Alineacion de celdas centrada para ID y Estado
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        instanceTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        instanceTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        instanceTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        instanceTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        instanceTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        instanceTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        instanceTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        instanceTable.getColumnModel().getColumn(5).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(instanceTable);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // Divisor vertical con proporciones armonicas
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cardsPanel, tableContainer);
        split.setResizeWeight(0.32);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /**
     * Actualiza los graficos y la tabla de instancias en el EDT.
     */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        int totalInUse = 0;
        int totalInstances = 0;
        long now = System.currentTimeMillis();

        for (ResourceType type : ResourceType.values()) {
            int inUse = resourceManager.getInUseCount(type);
            int available = resourceManager.getAvailableCount(type);
            int total = type.getDefaultCapacity();
            totalInUse += inUse;
            totalInstances += total;

            int percent = (int) Math.round(resourceManager.getUtilizationPercentage(type));
            JProgressBar bar = progressBars.get(type);
            if (bar != null) {
                bar.setValue(percent);
                bar.setString(percent + "% en uso (" + inUse + "/" + total + ")");
            }

            JLabel label = countLabels.get(type);
            if (label != null) {
                label.setText(available + " de " + total + " disponibles");
            }
        }

        globalSummaryLabel.setText(String.format(" | Total: %d | Ocupados: %d | Libres: %d",
                totalInstances, totalInUse, totalInstances - totalInUse));

        // Actualizar filas de la tabla de instancias segun el filtro
        String selectedFilter = (String) filterCombo.getSelectedItem();
        instanceModel.setRowCount(0);

        for (ResourceType type : ResourceType.values()) {
            if (selectedFilter != null && !selectedFilter.startsWith("Todos")
                    && !selectedFilter.equals(type.getDisplayName())) {
                continue;
            }

            for (ResourceInstance instance : resourceManager.getInstances(type)) {
                Order order = instance.getOccupiedBy();
                String orderId = (order != null) ? order.getId() : "-";
                String clientName = (order != null) ? order.getClient().getNombre() : "-";
                String timeInUse = "-";
                if (instance.isInUse() && instance.getUsageStartTime() > 0) {
                    long elapsedMs = Math.max(0, now - instance.getUsageStartTime());
                    timeInUse = (elapsedMs / 1000) + " s";
                }

                instanceModel.addRow(new Object[]{
                        instance.getId(),
                        type.getDisplayName(),
                        instance.isInUse() ? "En uso" : "Libre",
                        orderId,
                        clientName,
                        timeInUse
                });
            }
        }
    }
}

package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.concurrency.ResourceInstance;
import elmer.proyectofinal_sopes2.concurrency.ResourceManager;
import elmer.proyectofinal_sopes2.model.ResourceType;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;

/** Monitor visual de permisos e instancias fisicas de recursos. */
public final class ResourceMonitorPanel extends JPanel {

    private final ResourceManager resourceManager;
    private final Map<ResourceType, JProgressBar> bars = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, DefaultTableModel> models = new EnumMap<>(ResourceType.class);

    public ResourceMonitorPanel(ResourceManager resourceManager) {
        super(new GridLayout(0, 2, 8, 8));
        this.resourceManager = resourceManager;
        for (ResourceType type : ResourceType.values()) {
            add(createResourceView(type));
        }
        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /** Solicita el refresco en el hilo grafico. */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        for (ResourceType type : ResourceType.values()) {
            JProgressBar bar = bars.get(type);
            bar.setValue((int) Math.round(resourceManager.getUtilizationPercentage(type)));
            DefaultTableModel model = models.get(type);
            model.setRowCount(0);
            for (ResourceInstance instance : resourceManager.getInstances(type)) {
                model.addRow(new Object[]{instance.getId(), instance.isInUse() ? "En uso" : "Libre",
                        instance.getOccupiedBy() == null ? "-" : instance.getOccupiedBy().getId()});
            }
        }
    }

    private JPanel createResourceView(ResourceType type) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(type.getDisplayName()));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bars.put(type, bar);
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Instancia", "Estado", "Pedido"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        models.put(type, model);
        panel.add(new JLabel("Ocupacion"), BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        panel.add(new JScrollPane(new JTable(model)), BorderLayout.SOUTH);
        return panel;
    }
}
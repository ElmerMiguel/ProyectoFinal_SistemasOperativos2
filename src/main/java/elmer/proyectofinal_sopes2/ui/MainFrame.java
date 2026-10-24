package elmer.proyectofinal_sopes2.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import elmer.proyectofinal_sopes2.concurrency.DeadlockDetector;
import elmer.proyectofinal_sopes2.concurrency.DeadlockScenario;
import elmer.proyectofinal_sopes2.concurrency.WaitForGraph;
import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
import elmer.proyectofinal_sopes2.scheduler.SchedulingPolicy;
import elmer.proyectofinal_sopes2.simulation.SimulationController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Ventana principal integrada del simulador REDXela.
 * Organiza los monitores de recursos, seguimiento de pedidos, mapa de almacen
 * y estadisticas de rendimiento con soporte nativo LookAndFeel (FlatLaf).
 */
public final class MainFrame extends JFrame {

    private final SimulationController controller;

    private final JLabel clockStatus = new JLabel("Tiempo: 0 ms");
    private final JLabel simulationStatus = new JLabel("Estado: Detenida");
    private final JLabel policyStatus = new JLabel("Politica: Prioridad Estricta");
    private final JLabel bufferStatus = new JLabel("Buffer IPC: Conectado");

    private final JButton startPause = new JButton("Iniciar Simulacion");
    private final JComboBox<SchedulingPolicy> policySelector = new JComboBox<>(SchedulingPolicy.values());
    private final JComboBox<String> speedSelector = new JComboBox<>(new String[]{"1x (Normal)", "2x (Rapido)", "5x (Maximo)"});

    private ResourceMonitorPanel resourcePanel;
    private OrderTrackingPanel orderPanel;
    private WarehouseVisualizerPanel warehousePanel;
    private ReceptionPanel receptionPanel;
    private MetricsPanel metricsPanel;

    private boolean running;

    public MainFrame(SimulationController controller) {
        super("REDXela - Simulador de Centro de Distribucion (Sistemas Operativos 2)");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1280, 820));
        setMinimumSize(new Dimension(1100, 720));
        setLayout(new BorderLayout(0, 0));

        setJMenuBar(createMenuBar());
        add(createToolbar(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        new Timer(500, event -> refreshStatus()).start();
        pack();
        setLocationRelativeTo(null);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        startPause.setFont(startPause.getFont().deriveFont(Font.BOLD));
        startPause.addActionListener(event -> toggleSimulation());
        toolbar.add(startPause);
        toolbar.add(Box.createHorizontalStrut(8));

        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(Box.createHorizontalStrut(8));

        toolbar.add(new JLabel("Velocidad: "));
        speedSelector.addActionListener(event -> {
            int idx = speedSelector.getSelectedIndex();
            double mult = (idx == 0) ? 1.0 : (idx == 1) ? 2.0 : 5.0;
            controller.getClock().setSpeedMultiplier(mult);
        });
        toolbar.add(speedSelector);
        toolbar.add(Box.createHorizontalStrut(8));

        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(Box.createHorizontalStrut(8));

        toolbar.add(new JLabel("Politica: "));
        policySelector.setSelectedItem(controller.getScheduler().getPolicy());
        policySelector.addActionListener(event -> {
            SchedulingPolicy selected = (SchedulingPolicy) policySelector.getSelectedItem();
            if (selected != null) {
                controller.getScheduler().setPolicy(selected);
                policyStatus.setText("Politica: " + selected.getDisplayName());
            }
        });
        toolbar.add(policySelector);
        toolbar.add(Box.createHorizontalStrut(12));

        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(Box.createHorizontalStrut(12));

        JButton newOrder = new JButton("Nuevo Pedido");
        newOrder.addActionListener(event -> new NewOrderDialog(this, controller).setVisible(true));
        toolbar.add(newOrder);
        toolbar.add(Box.createHorizontalStrut(6));

        JButton newReception = new JButton("Recepcion de Stock");
        newReception.addActionListener(event -> new NewReceptionDialog(this, controller).setVisible(true));
        toolbar.add(newReception);
        toolbar.add(Box.createHorizontalStrut(6));

        JButton deadlock = new JButton("Provocar Interbloqueo");
        deadlock.addActionListener(event -> triggerDeadlock());
        toolbar.add(deadlock);

        return toolbar;
    }

    private JTabbedPane createTabs() {
        resourcePanel = new ResourceMonitorPanel(controller.getResourceManager());
        orderPanel = new OrderTrackingPanel(controller.getScheduler());
        warehousePanel = new WarehouseVisualizerPanel(controller.getInventoryManager().getWarehouse());
        receptionPanel = new ReceptionPanel(controller.getReceptionBuffer(), controller.getInventoryManager());
        metricsPanel = new MetricsPanel(controller.getMetrics());

        JTabbedPane tabs = new JTabbedPane();

        // Pestaña 1: Monitoreo General en división horizontal armónica (Recursos a la izquierda, Pedidos a la derecha)
        JSplitPane monitoring = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resourcePanel, orderPanel);
        monitoring.setResizeWeight(0.46);
        monitoring.setDividerSize(6);
        monitoring.setContinuousLayout(true);

        // Pestaña 2: Almacén e Inventario (Mapa a la izquierda, Buffer/Catálogo a la derecha)
        JSplitPane inventory = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, warehousePanel, receptionPanel);
        inventory.setResizeWeight(0.55);
        inventory.setDividerSize(6);
        inventory.setContinuousLayout(true);

        tabs.addTab("Monitoreo General (Recursos y Pedidos)", monitoring);
        tabs.addTab("Almacen Fisico e Inventario (Paginacion e IPC)", inventory);
        tabs.addTab("Estadisticas y Evaluacion de Politicas", metricsPanel);

        return tabs;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new GridLayout(1, 4, 12, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        clockStatus.setFont(clockStatus.getFont().deriveFont(Font.PLAIN, 11f));
        simulationStatus.setFont(simulationStatus.getFont().deriveFont(Font.PLAIN, 11f));
        policyStatus.setFont(policyStatus.getFont().deriveFont(Font.PLAIN, 11f));
        bufferStatus.setFont(bufferStatus.getFont().deriveFont(Font.PLAIN, 11f));

        bar.add(clockStatus);
        bar.add(simulationStatus);
        bar.add(policyStatus);
        bar.add(bufferStatus);
        return bar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Archivo
        JMenu file = new JMenu("Archivo");
        JMenuItem exit = new JMenuItem("Salir");
        exit.addActionListener(event -> dispose());
        file.add(exit);

        // Menu Simulacion
        JMenu simulation = new JMenu("Simulacion");
        JMenuItem miToggle = new JMenuItem("Iniciar / Pausar");
        miToggle.addActionListener(event -> toggleSimulation());
        JMenuItem reset = new JMenuItem("Reiniciar Metricas");
        reset.addActionListener(event -> {
            controller.reset();
            running = false;
            startPause.setText("Iniciar Simulacion");
            simulationStatus.setText("Estado: Reiniciada");
        });
        simulation.add(miToggle);
        simulation.add(reset);

        // Menu Apariencia (FlatLaf)
        JMenu appearance = new JMenu("Apariencia");
        JMenuItem darkTheme = new JMenuItem("Tema Oscuro (FlatDarcula)");
        darkTheme.addActionListener(e -> changeTheme(new FlatDarculaLaf()));
        JMenuItem lightTheme = new JMenuItem("Tema Claro (FlatLight)");
        lightTheme.addActionListener(e -> changeTheme(new FlatLightLaf()));
        JMenuItem intelliJTheme = new JMenuItem("Tema IntelliJ (FlatIntelliJ)");
        intelliJTheme.addActionListener(e -> changeTheme(new FlatIntelliJLaf()));
        appearance.add(darkTheme);
        appearance.add(lightTheme);
        appearance.add(intelliJTheme);

        // Menu Ayuda
        JMenu help = new JMenu("Ayuda");
        JMenuItem about = new JMenuItem("Acerca de REDXela...");
        about.addActionListener(event -> JOptionPane.showMessageDialog(this,
                """
                REDXela - Centro de Distribucion Concurrente
                Proyecto Final de Sistemas Operativos 2

                Conceptos Teóricos Implementados:
                • Planificación de CPU: Prioridad Estricta, Aging, SJF y FIFO.
                • Sincronización y Exclusión Mutua: 6 recursos compartidos finitos.
                • IPC con Buffer Acotado: Productor-Consumidor (Recepción / Inventario).
                • Almacenamiento no contiguo: Simulación de Paginación física.
                • Detección y Resolución de Deadlock: Ciclos en RAG (Condiciones de Coffman).
                """,
                "Acerca de REDXela", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        menuBar.add(file);
        menuBar.add(simulation);
        menuBar.add(appearance);
        menuBar.add(help);
        return menuBar;
    }

    private void changeTheme(javax.swing.LookAndFeel laf) {
        try {
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            System.err.println("No se pudo aplicar el tema: " + ex.getMessage());
        }
    }

    private void toggleSimulation() {
        if (running) {
            controller.pause();
            running = false;
            startPause.setText("Continuar Simulacion");
            simulationStatus.setText("Estado: Pausada");
        } else {
            controller.start();
            running = true;
            startPause.setText("Pausar Simulacion");
            simulationStatus.setText("Estado: En ejecucion");
        }
    }

    private void refreshStatus() {
        clockStatus.setText("Tiempo virtual: " + controller.getClock().getVirtualTimeMillis() + " ms");
        bufferStatus.setText(String.format("Buffer IPC: %d/%d lotes",
                controller.getReceptionBuffer().size(), controller.getReceptionBuffer().getCapacity()));
        resourcePanel.refresh();
        orderPanel.refresh();
        warehousePanel.refresh();
        receptionPanel.refresh();
        metricsPanel.refresh();
    }

    private void triggerDeadlock() {
        Product product = controller.getProducts().getFirst();
        Client client = new Client("DL-CLIENT", "Prueba Interbloqueo", ServiceLevel.NIVEL_1_EXPRES);
        Order operationA = new Order("DL-UI-A", client, ServiceLevel.NIVEL_1_EXPRES,
                List.of(new OrderItem(product, 1)), controller.getClock().getVirtualTimeMillis());
        Order operationB = new Order("DL-UI-B", client, ServiceLevel.NIVEL_1_EXPRES,
                List.of(new OrderItem(product, 1)), controller.getClock().getVirtualTimeMillis());

        DeadlockScenario scenario = new DeadlockScenario(controller.getResourceManager(),
                new DeadlockDetector(new WaitForGraph()));

        Thread worker = new Thread(() -> {
            try {
                var incident = scenario.triggerDeliberateDeadlock(operationA, operationB);
                SwingUtilities.invokeLater(() -> new DeadlockResolutionDialog(this, scenario, incident).setVisible(true));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "deadlock-ui-trigger");
        worker.start();
    }
}

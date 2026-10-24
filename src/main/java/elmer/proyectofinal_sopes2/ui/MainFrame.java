package elmer.proyectofinal_sopes2.ui;

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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/** Ventana principal que integra monitores y controles de simulacion. */
public final class MainFrame extends JFrame {

    private final SimulationController controller;
    private final JLabel clockStatus = new JLabel();
    private final JLabel simulationStatus = new JLabel("Detenida");
    private final JButton startPause = new JButton("Iniciar");
    private ResourceMonitorPanel resourcePanel;
    private OrderTrackingPanel orderPanel;
    private WarehouseVisualizerPanel warehousePanel;
    private ReceptionPanel receptionPanel;
    private MetricsPanel metricsPanel;
    private boolean running;

    public MainFrame(SimulationController controller) {
        super("REDXela - Centro de Distribucion");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setLayout(new BorderLayout(8, 8));
        add(createToolbar(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(createMenuBar());
        new Timer(500, event -> refreshStatus()).start();
        pack();
        setLocationRelativeTo(null);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        startPause.addActionListener(event -> toggleSimulation());
        toolbar.add(startPause);
        JComboBox<String> speed = new JComboBox<>(new String[]{"1x", "2x", "5x"});
        speed.addActionListener(event -> controller.getClock().setSpeedMultiplier(speed.getSelectedIndex() == 0 ? 1 : speed.getSelectedIndex() == 1 ? 2 : 5));
        toolbar.add(new JLabel(" Velocidad: "));
        toolbar.add(speed);
        JComboBox<SchedulingPolicy> policy = new JComboBox<>(SchedulingPolicy.values());
        policy.setSelectedItem(controller.getScheduler().getPolicy());
        policy.addActionListener(event -> controller.getScheduler().setPolicy((SchedulingPolicy) policy.getSelectedItem()));
        toolbar.add(new JLabel(" Politica: "));
        toolbar.add(policy);
        JButton newOrder = new JButton("Nuevo pedido");
        newOrder.addActionListener(event -> new NewOrderDialog(this, controller).setVisible(true));
        toolbar.add(newOrder);
        JButton newReception = new JButton("Recepcion manual");
        newReception.addActionListener(event -> new NewReceptionDialog(this, controller).setVisible(true));
        toolbar.add(newReception);
        JButton deadlock = new JButton("Provocar interbloqueo");
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
        JSplitPane monitoring = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resourcePanel, orderPanel);
        monitoring.setResizeWeight(0.55);
        JSplitPane inventory = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, warehousePanel, receptionPanel);
        inventory.setResizeWeight(0.65);
        tabs.addTab("Monitoreo general", monitoring);
        tabs.addTab("Almacen e inventario", inventory);
        tabs.addTab("Estadisticas", metricsPanel);
        return tabs;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new GridLayout(1, 3, 8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bar.add(clockStatus);
        bar.add(simulationStatus);
        bar.add(new JLabel("Buffer IPC: conectado"));
        return bar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("Archivo");
        JMenuItem exit = new JMenuItem("Salir");
        exit.addActionListener(event -> dispose());
        file.add(exit);
        JMenu simulation = new JMenu("Simulacion");
        JMenuItem reset = new JMenuItem("Reiniciar");
        reset.addActionListener(event -> { controller.reset(); running = false; startPause.setText("Iniciar"); });
        simulation.add(reset);
        JMenu help = new JMenu("Ayuda");
        help.add(new JMenuItem("REDXela - Sistemas Operativos 2"));
        menuBar.add(file); menuBar.add(simulation); menuBar.add(help);
        return menuBar;
    }

    private void toggleSimulation() {
        if (running) {
            controller.pause();
            running = false;
            startPause.setText("Continuar");
            simulationStatus.setText("Pausada");
        } else {
            controller.start();
            running = true;
            startPause.setText("Pausar");
            simulationStatus.setText("En ejecucion");
        }
    }

    private void refreshStatus() {
        clockStatus.setText("Tiempo virtual: " + controller.getClock().getVirtualTimeMillis() + " ms");
        resourcePanel.refresh();
        orderPanel.refresh();
        warehousePanel.refresh();
        receptionPanel.refresh();
        metricsPanel.refresh();
    }

    private void triggerDeadlock() {
        Product product = controller.getProducts().get(0);
        Client client = new Client("DL-CLIENT", "Prueba deadlock", ServiceLevel.NIVEL_1_EXPRES);
        Order operationA = new Order("DL-UI-A", client, ServiceLevel.NIVEL_1_EXPRES,
                List.of(new OrderItem(product, 1)), controller.getClock().getVirtualTimeMillis());
        Order operationB = new Order("DL-UI-B", client, ServiceLevel.NIVEL_1_EXPRES,
                List.of(new OrderItem(product, 1)), controller.getClock().getVirtualTimeMillis());
        DeadlockScenario scenario = new DeadlockScenario(controller.getResourceManager(),
                new DeadlockDetector(new WaitForGraph()));
        Thread worker = new Thread(() -> {
            try {
                var incident = scenario.triggerDeliberateDeadlock(operationA, operationB);
                javax.swing.SwingUtilities.invokeLater(() -> new DeadlockResolutionDialog(this, scenario, incident).setVisible(true));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "deadlock-ui-trigger");
        worker.start();
    }
}
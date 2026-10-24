package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.ipc.ReceptionBuffer;
import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.storage.InventoryManager;

import javax.swing.BorderFactory;
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
import java.util.List;

/**
 * Monitor visual de la rampa de recepcion (Buffer IPC acotado)
 * y catalogo de inventario físico en tiempo real.
 */
public final class ReceptionPanel extends JPanel {

    private final ReceptionBuffer buffer;
    private final InventoryManager inventory;

    private final JProgressBar bufferBar = new JProgressBar();
    private final JLabel bufferStatusLabel = new JLabel();

    private final DefaultTableModel batchesModel;
    private final DefaultTableModel productModel;

    public ReceptionPanel(ReceptionBuffer buffer, InventoryManager inventory) {
        super(new BorderLayout(8, 8));
        this.buffer = buffer;
        this.inventory = inventory;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Panel Superior: Buffer IPC de Recepcion
        JPanel bufferContainer = new JPanel(new BorderLayout(4, 4));
        bufferContainer.setBorder(BorderFactory.createTitledBorder("Rampa de Descarga y Buffer IPC (Productor -> Consumidor)"));

        JPanel bufferHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 4));
        bufferBar.setPreferredSize(new Dimension(200, 20));
        bufferBar.setStringPainted(true);
        bufferHeader.add(new JLabel("Ocupacion Buffer:"));
        bufferHeader.add(bufferBar);
        bufferHeader.add(bufferStatusLabel);
        bufferContainer.add(bufferHeader, BorderLayout.NORTH);

        String[] batchCols = {"ID Lote", "Producto Recibido", "Cantidad", "Camion / Manifiesto", "Tiempo Llegada"};
        batchesModel = createModel(batchCols);
        JTable batchesTable = configureTable(batchesModel);
        bufferContainer.add(new JScrollPane(batchesTable), BorderLayout.CENTER);

        // 2. Panel Inferior: Catalogo de Inventario
        JPanel catalogContainer = new JPanel(new BorderLayout(4, 4));
        catalogContainer.setBorder(BorderFactory.createTitledBorder("Catalogo de Productos y Stock Disponible en Almacen"));

        String[] prodCols = {"Codigo", "Nombre del Articulo", "Tipo de Mercancia", "Stock Disponible", "Unidades / Casilla"};
        productModel = createModel(prodCols);
        JTable productTable = configureTable(productModel);
        catalogContainer.add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Split vertical
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, bufferContainer, catalogContainer);
        split.setResizeWeight(0.45);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /**
     * Refresca las listas y estado de colas en el EDT.
     */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        int size = buffer.size();
        int capacity = buffer.getCapacity();
        bufferBar.setMaximum(capacity);
        bufferBar.setValue(size);
        int percent = (capacity > 0) ? (size * 100 / capacity) : 0;
        bufferBar.setString(size + " / " + capacity + " lotes (" + percent + "%)");
        bufferStatusLabel.setText(String.format(" | Capacidad disponible: %d lotes", buffer.remainingCapacity()));

        // Actualizar lotes pendientes en el buffer
        batchesModel.setRowCount(0);
        List<StockArrivalBatch> batches = buffer.getPendingBatches();
        for (StockArrivalBatch batch : batches) {
            batchesModel.addRow(new Object[]{
                    batch.batchId(),
                    batch.producto().getNombre(),
                    batch.cantidadRecibida() + " u",
                    batch.camionOrigen(),
                    batch.timestampLlegada() + " ms"
            });
        }

        // Actualizar stock de productos
        productModel.setRowCount(0);
        for (Product product : inventory.getProducts()) {
            productModel.addRow(new Object[]{
                    product.getId(),
                    product.getNombre(),
                    product.getTipoMercancia().getDisplayName(),
                    product.getStockDisponible() + " unidades",
                    product.getUnidadesPorSlot() + " u/slot"
            });
        }
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
        if (table.getColumnCount() >= 4) {
            table.getColumnModel().getColumn(2).setCellRenderer(center);
            table.getColumnModel().getColumn(3).setCellRenderer(center);
        }
        return table;
    }
}

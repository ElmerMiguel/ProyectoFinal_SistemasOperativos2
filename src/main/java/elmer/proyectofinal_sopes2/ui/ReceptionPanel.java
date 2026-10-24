package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.ipc.ReceptionBuffer;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.storage.InventoryManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Monitor de capacidad IPC y catalogo de inventario. */
public final class ReceptionPanel extends JPanel {

    private final ReceptionBuffer buffer;
    private final InventoryManager inventory;
    private final JLabel bufferStatus = new JLabel();
    private final DefaultTableModel productModel = new DefaultTableModel(new Object[]{"ID", "Producto", "Tipo", "Stock"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public ReceptionPanel(ReceptionBuffer buffer, InventoryManager inventory) {
        super(new BorderLayout(8, 8));
        this.buffer = buffer;
        this.inventory = inventory;
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createTitledBorder("Buffer IPC"));
        header.add(bufferStatus);
        add(header, BorderLayout.NORTH);
        JPanel catalog = new JPanel(new BorderLayout());
        catalog.setBorder(BorderFactory.createTitledBorder("Catalogo de productos"));
        catalog.add(new JScrollPane(new JTable(productModel)));
        add(catalog, BorderLayout.CENTER);
        new Timer(1_000, ignored -> refresh()).start();
        refresh();
    }

    /** Refresca buffer y stock en el EDT. */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        bufferStatus.setText(String.format("Lotes en espera: %d de %d", buffer.size(),
                buffer.size() + buffer.remainingCapacity()));
        productModel.setRowCount(0);
        for (Product product : inventory.getProducts()) {
            productModel.addRow(new Object[]{product.getId(), product.getNombre(),
                    product.getTipoMercancia().getDisplayName(), product.getStockDisponible()});
        }
    }
}
package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.StorageSlot;
import elmer.proyectofinal_sopes2.storage.WarehouseGrid;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.util.List;

/** Mapa nativo de casillas con detalle accesible por tooltip. */
public final class WarehouseVisualizerPanel extends JPanel {

    private final WarehouseGrid warehouse;
    private final JPanel grid = new JPanel(new GridLayout(0, 5, 3, 3));

    public WarehouseVisualizerPanel(WarehouseGrid warehouse) {
        super(new java.awt.BorderLayout());
        this.warehouse = warehouse;
        add(new JScrollPane(grid), java.awt.BorderLayout.CENTER);
        refresh();
    }

    /** Redibuja la tabla de paginas en el EDT. */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        grid.removeAll();
        List<StorageSlot> slots = warehouse.getSlots();
        for (StorageSlot slot : slots) {
            JButton cell = new JButton(slot.isEmpty() ? "Libre" : slot.getUnidadesOcupadas() + " u");
            cell.setToolTipText(detail(slot));
            grid.add(cell);
        }
        grid.revalidate();
        grid.repaint();
    }

    private static String detail(StorageSlot slot) {
        String product = slot.getProductoActual() == null ? "-" : slot.getProductoActual().getNombre();
        return String.format("%s | Producto: %s | Lote: %s | %d/%d unidades",
                slot.getUbicacionCodigo(), product, slot.getLoteId(),
                slot.getUnidadesOcupadas(), slot.getCapacidadMaxima());
    }
}
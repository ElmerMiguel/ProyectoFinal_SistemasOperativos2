package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.StorageSlot;
import elmer.proyectofinal_sopes2.storage.WarehouseGrid;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizador grafico interactivo del almacen fisico (Asignacion no contigua / Paginacion).
 * Permite explorar por pasillos y estantes, observando la fragmentacion y ubicacion de lotes.
 */
public final class WarehouseVisualizerPanel extends JPanel {

    private final WarehouseGrid warehouse;
    private final JComboBox<String> aisleSelector;
    private final JLabel statsLabel = new JLabel();
    private final JLabel detailLabel = new JLabel("Haz clic en cualquier casilla para ver los detalles.");
    private final JPanel racksContainer = new JPanel();
    private final List<SlotButton> slotButtons = new ArrayList<>();

    public WarehouseVisualizerPanel(WarehouseGrid warehouse) {
        super(new BorderLayout(8, 8));
        this.warehouse = warehouse;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Barra de control superior
        JPanel controlBar = new JPanel(new BorderLayout(8, 4));
        controlBar.setBorder(BorderFactory.createTitledBorder("Control de Navegacion del Almacen"));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 4));
        leftControls.add(new JLabel("Seleccionar Pasillo:"));
        aisleSelector = new JComboBox<>(new String[]{
                "Pasillo 1", "Pasillo 2", "Pasillo 3", "Pasillo 4", "Pasillo 5", "Todos los Pasillos (Vista General)"
        });
        aisleSelector.addActionListener(e -> rebuildView());
        leftControls.add(aisleSelector);

        controlBar.add(leftControls, BorderLayout.WEST);
        controlBar.add(statsLabel, BorderLayout.EAST);
        add(controlBar, BorderLayout.NORTH);

        // 2. Contenedor central con scroll
        JScrollPane scrollPane = new JScrollPane(racksContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // 3. Barra de detalle inferior
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        detailLabel.setFont(detailLabel.getFont().deriveFont(Font.BOLD, 12f));
        bottomPanel.add(detailLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        rebuildView();
        new Timer(1_000, ignored -> refresh()).start();
    }

    /**
     * Construye la vista de estantes según el pasillo seleccionado.
     */
    private void rebuildView() {
        racksContainer.removeAll();
        slotButtons.clear();

        int selectedIndex = aisleSelector.getSelectedIndex(); // 0 a 4 = Pasillo 1-5, 5 = Todos

        if (selectedIndex >= 0 && selectedIndex < 5) {
            int aisle = selectedIndex + 1;
            // 4 estantes en este pasillo, mostrados en cuadrícula 2x2
            racksContainer.setLayout(new GridLayout(2, 2, 10, 10));

            for (int shelf = 1; shelf <= WarehouseGrid.DEFAULT_SHELVES; shelf++) {
                JPanel shelfPanel = new JPanel(new BorderLayout(4, 4));
                shelfPanel.setBorder(BorderFactory.createTitledBorder("Estante " + shelf + " (Pasillo " + aisle + ")"));

                // 3 niveles x 5 casillas
                JPanel shelfGrid = new JPanel(new GridLayout(WarehouseGrid.DEFAULT_LEVELS, WarehouseGrid.DEFAULT_SLOTS, 4, 4));
                shelfGrid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

                // Niveles de arriba hacia abajo (3, 2, 1)
                for (int level = WarehouseGrid.DEFAULT_LEVELS; level >= 1; level--) {
                    for (int slotNum = 1; slotNum <= WarehouseGrid.DEFAULT_SLOTS; slotNum++) {
                        StorageSlot slot = findSlot(aisle, shelf, level, slotNum);
                        if (slot != null) {
                            SlotButton btn = new SlotButton(slot);
                            slotButtons.add(btn);
                            shelfGrid.add(btn);
                        }
                    }
                }
                shelfPanel.add(shelfGrid, BorderLayout.CENTER);
                racksContainer.add(shelfPanel);
            }
        } else {
            // Vista general de todos los pasillos
            racksContainer.setLayout(new GridLayout(0, 10, 3, 3));
            for (StorageSlot slot : warehouse.getSlots()) {
                SlotButton btn = new SlotButton(slot);
                slotButtons.add(btn);
                racksContainer.add(btn);
            }
        }

        racksContainer.revalidate();
        racksContainer.repaint();
        refresh();
    }

    private StorageSlot findSlot(int aisle, int shelf, int level, int slotNum) {
        for (StorageSlot slot : warehouse.getSlots()) {
            if (slot.getPasillo() == aisle && slot.getEstante() == shelf
                    && slot.getNivel() == level && slot.getCasilla() == slotNum) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Actualiza el estado visual de los botones existentes sin reconstruir el árbol de componentes.
     */
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        int totalSlots = warehouse.getSlots().size();
        int occupiedSlots = 0;
        int totalUnits = 0;

        for (StorageSlot slot : warehouse.getSlots()) {
            if (!slot.isEmpty()) {
                occupiedSlots++;
                totalUnits += slot.getUnidadesOcupadas();
            }
        }

        double percentOccupied = (totalSlots > 0) ? (occupiedSlots * 100.0 / totalSlots) : 0;
        statsLabel.setText(String.format("Total: %d casillas | Ocupadas: %d (%.1f%%) | Libres: %d | Unidades almacenadas: %d  ",
                totalSlots, occupiedSlots, percentOccupied, totalSlots - occupiedSlots, totalUnits));

        for (SlotButton btn : slotButtons) {
            btn.updateState();
        }
    }

    private final class SlotButton extends JButton {
        private final StorageSlot slot;

        private SlotButton(StorageSlot slot) {
            this.slot = slot;
            setFont(getFont().deriveFont(10f));
            setPreferredSize(new Dimension(85, 34));
            addActionListener(e -> showDetail());
            updateState();
        }

        private void updateState() {
            if (slot.isEmpty()) {
                setText(String.format("<html><center><small>%s</small><br>Libre</center></html>",
                        slot.getUbicacionCodigo()));
            } else {
                Product p = slot.getProductoActual();
                String name = (p != null) ? p.getNombre() : "Item";
                if (name.length() > 9) name = name.substring(0, 8) + "..";
                setText(String.format("<html><center><small>%s</small><br><b>%s (%d)</b></center></html>",
                        slot.getUbicacionCodigo(), name, slot.getUnidadesOcupadas()));
            }
            setToolTipText(createTooltip());
        }

        private String createTooltip() {
            if (slot.isEmpty()) {
                return String.format("[%s] Casilla Libre (0/%d unidades)",
                        slot.getUbicacionCodigo(), slot.getCapacidadMaxima());
            }
            Product p = slot.getProductoActual();
            return String.format("<html><b>Ubicacion:</b> %s<br><b>Producto:</b> %s (%s)<br><b>Lote:</b> %s<br><b>Ocupacion:</b> %d / %d unidades</html>",
                    slot.getUbicacionCodigo(),
                    (p != null) ? p.getNombre() : "-",
                    (p != null) ? p.getTipoMercancia().getDisplayName() : "-",
                    slot.getLoteId(),
                    slot.getUnidadesOcupadas(),
                    slot.getCapacidadMaxima());
        }

        private void showDetail() {
            if (slot.isEmpty()) {
                detailLabel.setText(String.format("Ubicacion: %s | Estado: LIBRE (Capacidad disponible: %d unidades)",
                        slot.getUbicacionCodigo(), slot.getCapacidadMaxima()));
            } else {
                Product p = slot.getProductoActual();
                detailLabel.setText(String.format("Ubicacion: %s | Producto: %s [%s] | Lote: %s | Ocupacion: %d/%d unidades",
                        slot.getUbicacionCodigo(),
                        (p != null) ? p.getNombre() : "-",
                        (p != null) ? p.getTipoMercancia().getDisplayName() : "-",
                        slot.getLoteId(),
                        slot.getUnidadesOcupadas(),
                        slot.getCapacidadMaxima()));
            }
        }
    }
}

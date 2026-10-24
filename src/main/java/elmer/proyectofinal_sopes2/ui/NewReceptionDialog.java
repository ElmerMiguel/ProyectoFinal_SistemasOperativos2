package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.simulation.SimulationController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Dialogo modal interactivo para ingresar manualmente un lote de mercadería al buffer IPC.
 */
public final class NewReceptionDialog extends JDialog {

    public NewReceptionDialog(MainFrame owner, SimulationController controller) {
        super(owner, "Registrar Llegada de Mercaderia (Stock Manual)", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 14, 8, 14),
                BorderFactory.createTitledBorder("Manifiesto de Carga y Lote")
        ));

        JComboBox<Product> product = new JComboBox<>(controller.getProducts().toArray(Product[]::new));
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 5));
        JTextField lot = new JTextField("LOTE-MANUAL-" + (System.currentTimeMillis() % 1000));
        JTextField truck = new JTextField("CAMION-XELA-" + (1 + (int)(Math.random() * 99)));

        form.add(new JLabel("Producto a Recibir:"));
        form.add(product);
        form.add(new JLabel("Cantidad de Unidades:"));
        form.add(quantity);
        form.add(new JLabel("Identificador de Lote:"));
        form.add(lot);
        form.add(new JLabel("Camion / Proveedor:"));
        form.add(truck);

        JButton receive = new JButton("Ingresar a Recepcion");
        receive.addActionListener(event -> {
            try {
                Product selected = (Product) product.getSelectedItem();
                if (selected != null) {
                    controller.receiveManualStock(new StockArrivalBatch(
                            lot.getText().trim(),
                            controller.getClock().getVirtualTimeMillis(),
                            selected,
                            (Integer) quantity.getValue(),
                            truck.getText().trim()
                    ));
                }
                dispose();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                dispose();
            }
        });

        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 8));
        actions.add(cancel);
        actions.add(receive);

        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        setSize(new Dimension(480, 260));
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

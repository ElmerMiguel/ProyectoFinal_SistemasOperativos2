package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.ipc.StockArrivalBatch;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.simulation.SimulationController;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** Dialogo modal para registrar un lote de recepcion. */
public final class NewReceptionDialog extends JDialog {

    public NewReceptionDialog(MainFrame owner, SimulationController controller) {
        super(owner, "Recepcion manual", Dialog.ModalityType.APPLICATION_MODAL);
        JComboBox<Product> product = new JComboBox<>(controller.getProducts().toArray(Product[]::new));
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
        JTextField lot = new JTextField("LOT-MANUAL");
        JTextField truck = new JTextField("TRUCK-MANUAL");
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Producto")); form.add(product);
        form.add(new JLabel("Unidades")); form.add(quantity);
        form.add(new JLabel("Lote")); form.add(lot);
        form.add(new JLabel("Camion")); form.add(truck);
        JButton receive = new JButton("Ingresar a recepcion");
        receive.addActionListener(event -> {
            try {
                controller.receiveManualStock(new StockArrivalBatch(lot.getText(), controller.getClock().getVirtualTimeMillis(),
                        (Product) product.getSelectedItem(), (Integer) quantity.getValue(), truck.getText()));
                dispose();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                dispose();
            }
        });
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(cancel); actions.add(receive);
        add(form, BorderLayout.CENTER); add(actions, BorderLayout.SOUTH);
        setSize(430, 260);
        setLocationRelativeTo(owner);
    }
}
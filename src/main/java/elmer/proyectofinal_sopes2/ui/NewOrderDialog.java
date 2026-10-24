package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.model.Client;
import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.OrderItem;
import elmer.proyectofinal_sopes2.model.Product;
import elmer.proyectofinal_sopes2.model.ServiceLevel;
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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/** Dialogo modal para crear un pedido manual de una linea. */
public final class NewOrderDialog extends JDialog {

    public NewOrderDialog(MainFrame owner, SimulationController controller) {
        super(owner, "Nuevo pedido", Dialog.ModalityType.APPLICATION_MODAL);
        JTextField clientName = new JTextField("Cliente manual");
        JComboBox<ServiceLevel> level = new JComboBox<>(ServiceLevel.values());
        JComboBox<Product> product = new JComboBox<>(controller.getProducts().toArray(Product[]::new));
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JPanelForm form = new JPanelForm();
        form.addField("Cliente", clientName);
        form.addField("Nivel", level);
        form.addField("Producto", product);
        form.addField("Unidades", quantity);
        JButton create = new JButton("Crear y encolar");
        create.addActionListener(event -> {
            Client client = new Client("CLI-MANUAL-" + System.nanoTime(), clientName.getText(), (ServiceLevel) level.getSelectedItem());
            Product selected = (Product) product.getSelectedItem();
            Order order = new Order(client, (ServiceLevel) level.getSelectedItem(),
                    List.of(new OrderItem(selected, (Integer) quantity.getValue())), controller.getClock().getVirtualTimeMillis());
            controller.createManualOrder(order);
            dispose();
        });
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(cancel);
        actions.add(create);
        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        setSize(420, 260);
        setLocationRelativeTo(owner);
    }

    private static final class JPanelForm extends javax.swing.JPanel {
        private JPanelForm() {
            super(new GridLayout(0, 2, 6, 6));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        private void addField(String label, java.awt.Component component) {
            add(new JLabel(label));
            add(component);
        }
    }
}
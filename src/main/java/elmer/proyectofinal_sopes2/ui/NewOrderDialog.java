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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Dialogo modal interactivo para crear manualmente un pedido en REDXela.
 */
public final class NewOrderDialog extends JDialog {

    public NewOrderDialog(MainFrame owner, SimulationController controller) {
        super(owner, "Registrar Nuevo Pedido Manual", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 14, 8, 14),
                BorderFactory.createTitledBorder("Parametros del Pedido")
        ));

        JTextField clientName = new JTextField("Cliente Especial");
        JComboBox<ServiceLevel> level = new JComboBox<>(ServiceLevel.values());
        JComboBox<Product> product = new JComboBox<>(controller.getProducts().toArray(Product[]::new));
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(5, 1, 500, 1));

        form.add(new JLabel("Nombre del Cliente:"));
        form.add(clientName);
        form.add(new JLabel("Nivel de Servicio:"));
        form.add(level);
        form.add(new JLabel("Producto Solicitado:"));
        form.add(product);
        form.add(new JLabel("Cantidad de Unidades:"));
        form.add(quantity);

        JButton create = new JButton("Crear y Encolar Pedido");
        create.addActionListener(event -> {
            Client client = new Client("CLI-MANUAL-" + System.currentTimeMillis() % 10000,
                    clientName.getText().trim(), (ServiceLevel) level.getSelectedItem());
            Product selected = (Product) product.getSelectedItem();
            if (selected != null) {
                Order order = new Order(client, (ServiceLevel) level.getSelectedItem(),
                        List.of(new OrderItem(selected, (Integer) quantity.getValue())),
                        controller.getClock().getVirtualTimeMillis());
                controller.createManualOrder(order);
            }
            dispose();
        });

        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 8));
        actions.add(cancel);
        actions.add(create);

        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        setSize(new Dimension(480, 260));
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

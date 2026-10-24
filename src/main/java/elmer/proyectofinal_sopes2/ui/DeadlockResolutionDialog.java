package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.concurrency.DeadlockIncident;
import elmer.proyectofinal_sopes2.concurrency.DeadlockScenario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Dialogo modal interactivo para visualizar y resolver situaciones de interbloqueo (Deadlock).
 * Muestra el ciclo en el Grafo de Asignacion de Recursos y permite elegir que operacion continua.
 */
public final class DeadlockResolutionDialog extends JDialog {

    public DeadlockResolutionDialog(MainFrame owner, DeadlockScenario scenario, DeadlockIncident incident) {
        super(owner, "ALERTA: Interbloqueo Detectado (Espera Circular)", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(12, 12));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Titulo de advertencia
        JLabel alertTitle = new JLabel("¡CONFLICTO DE RECURSOS DETECTADO (DEADLOCK)!");
        alertTitle.setFont(alertTitle.getFont().deriveFont(Font.BOLD, 14f));
        content.add(alertTitle, BorderLayout.NORTH);

        // Explicacion detallada de Coffman
        JPanel details = new JPanel(new GridLayout(0, 1, 6, 6));
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Grafo de Asignacion y Espera Circular"),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        details.add(new JLabel("• Operación A (" + incident.operationA().getId() + "): Retiene ["
                + incident.heldByA().getDisplayName() + "] y espera [" + incident.requestedByA().getDisplayName() + "]."));
        details.add(new JLabel("• Operación B (" + incident.operationB().getId() + "): Retiene ["
                + incident.heldByB().getDisplayName() + "] y espera [" + incident.requestedByB().getDisplayName() + "]."));
        details.add(new JLabel("• Condición de Coffman: Exclusión mutua + Retención y espera + Sin apropiación + Espera circular."));
        details.add(new JLabel("Selecciona qué operación mantendrá los recursos para que la otra sea liberada y vuelva a la cola:"));
        content.add(details, BorderLayout.CENTER);

        // Botones de decision
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        JButton continueA = new JButton("Continuar con Operación A (" + incident.operationA().getId() + ")");
        continueA.setFont(continueA.getFont().deriveFont(Font.BOLD, 12f));
        continueA.addActionListener(event -> {
            scenario.resolve(incident, incident.operationA());
            dispose();
        });

        JButton continueB = new JButton("Continuar con Operación B (" + incident.operationB().getId() + ")");
        continueB.setFont(continueB.getFont().deriveFont(Font.BOLD, 12f));
        continueB.addActionListener(event -> {
            scenario.resolve(incident, incident.operationB());
            dispose();
        });

        actions.add(continueA);
        actions.add(continueB);
        content.add(actions, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        setSize(new Dimension(660, 270));
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

package elmer.proyectofinal_sopes2.ui;

import elmer.proyectofinal_sopes2.concurrency.DeadlockIncident;
import elmer.proyectofinal_sopes2.concurrency.DeadlockScenario;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;

/** Dialogo modal para seleccionar la victima de un interbloqueo. */
public final class DeadlockResolutionDialog extends JDialog {

    public DeadlockResolutionDialog(MainFrame owner, DeadlockScenario scenario, DeadlockIncident incident) {
        super(owner, "Interbloqueo detectado", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel details = new JPanel(new GridLayout(0, 1, 4, 4));
        details.add(new JLabel("Espera circular detectada entre dos operaciones."));
        details.add(new JLabel("A retiene " + incident.heldByA() + " y solicita " + incident.requestedByA()));
        details.add(new JLabel("B retiene " + incident.heldByB() + " y solicita " + incident.requestedByB()));
        details.add(new JLabel("Coffman: exclusion mutua, retencion y espera, no apropiacion y espera circular."));
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 8));
        JButton continueA = new JButton("Continuar A");
        continueA.addActionListener(event -> { scenario.resolve(incident, incident.operationA()); dispose(); });
        JButton continueB = new JButton("Continuar B");
        continueB.addActionListener(event -> { scenario.resolve(incident, incident.operationB()); dispose(); });
        actions.add(continueA); actions.add(continueB);
        add(details, BorderLayout.CENTER); add(actions, BorderLayout.SOUTH);
        setSize(620, 230);
        setLocationRelativeTo(owner);
    }
}
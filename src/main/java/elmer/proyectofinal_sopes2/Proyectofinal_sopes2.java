package elmer.proyectofinal_sopes2;

import com.formdev.flatlaf.FlatDarculaLaf;
import elmer.proyectofinal_sopes2.simulation.SimulationController;
import elmer.proyectofinal_sopes2.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de la aplicacion Swing REDXela.
 */
public class Proyectofinal_sopes2 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarculaLaf.setup();
            } catch (Exception exception) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
            }
            new MainFrame(new SimulationController()).setVisible(true);
        });
    }
}

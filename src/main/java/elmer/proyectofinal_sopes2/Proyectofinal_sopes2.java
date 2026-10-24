
package elmer.proyectofinal_sopes2;

import elmer.proyectofinal_sopes2.simulation.SimulationController;
import elmer.proyectofinal_sopes2.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de la aplicacion Swing.
 */
public class Proyectofinal_sopes2 {

    public static void main(String[] args) {
        configureLookAndFeel();
        SwingUtilities.invokeLater(() -> new MainFrame(new SimulationController()).setVisible(true));
    }

    private static void configureLookAndFeel() {
        try {
            Class<?> flatLaf = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLaf.getMethod("setup").invoke(null);
        } catch (ReflectiveOperationException exception) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing conserva su LookAndFeel por defecto.
            }
        }
    }
}

package ma.soutenance;

import ma.soutenance.ui.MainFrame;
import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;

public class MainApp {

    public static void main(String[] args) {
        AppTheme.applyLaF();

        // Lancer directement l'UI — pas de chargement Excel au démarrage
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
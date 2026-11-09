package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/** Barre de statut inférieure : voyant d'état coloré, message et progression. */
public class StatusBar extends JPanel {

    private enum State { IDLE, OK, ERR, BUSY }

    private final JLabel       msgLabel;
    private final JProgressBar progress;
    private final JComponent   dot;
    private State state = State.IDLE;

    public StatusBar() {
        setLayout(new BorderLayout(10, 0));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(0, AppTheme.STATUS_H));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
            new EmptyBorder(0, 20, 0, 20)));

        dot = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
                Color c = switch (state) {
                    case OK -> AppTheme.SUCCESS;
                    case ERR -> AppTheme.DANGER;
                    case BUSY -> AppTheme.ROSE;
                    default -> AppTheme.SOFT_PINK;
                };
                g2.setColor(AppTheme.alpha(c, 55));
                g2.fill(new Ellipse2D.Double(0, 0, 14, 14));
                g2.setColor(c);
                g2.fill(new Ellipse2D.Double(3.5, 3.5, 7, 7));
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(14, 14));
        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.add(dot);

        msgLabel = new JLabel("Prêt");
        msgLabel.setFont(AppTheme.F_SMALL);
        msgLabel.setForeground(AppTheme.TEXT_SECONDARY);

        progress = new JProgressBar();
        progress.setIndeterminate(false);
        progress.setVisible(false);
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(140, 6));
        JPanel progWrap = new JPanel(new GridBagLayout());
        progWrap.setOpaque(false);
        progWrap.add(progress);

        add(dotWrap,  BorderLayout.WEST);
        add(msgLabel, BorderLayout.CENTER);
        add(progWrap, BorderLayout.EAST);
    }

    private void setState(State s) { state = s; dot.repaint(); }

    public void setMessage(String msg) {
        SwingUtilities.invokeLater(() -> msgLabel.setText(msg));
    }

    public void setRunning(boolean running) {
        SwingUtilities.invokeLater(() -> {
            progress.setVisible(running);
            progress.setIndeterminate(running);
        });
    }

    public void ok(String msg)   { SwingUtilities.invokeLater(() -> setState(State.OK));   setMessage(msg); setRunning(false); }
    public void err(String msg)  { SwingUtilities.invokeLater(() -> setState(State.ERR));  setMessage(msg); setRunning(false); }
    public void busy(String msg) { SwingUtilities.invokeLater(() -> setState(State.BUSY)); setMessage(msg); setRunning(true); }
}

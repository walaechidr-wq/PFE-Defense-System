package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Barre de progression fine à bouts arrondis (rose → crimson). */
public class ThemedProgressBarUI extends BasicProgressBarUI {

    public static ComponentUI createUI(JComponent c) { return new ThemedProgressBarUI(); }

    @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(140, 6); }

    private void track(Graphics2D g2, JComponent c) {
        g2.setColor(AppTheme.tint(AppTheme.SOFT_PINK, .6));
        g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), c.getHeight(), c.getHeight()));
    }

    @Override protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        track(g2, c);
        Rectangle box = getBox(null);
        if (box != null) {
            g2.setPaint(new GradientPaint(box.x, 0, AppTheme.ROSE, box.x + box.width, 0, AppTheme.CRIMSON));
            g2.fill(new RoundRectangle2D.Double(box.x, 0, box.width, c.getHeight(), c.getHeight(), c.getHeight()));
        }
        g2.dispose();
    }

    @Override protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        track(g2, c);
        int w = (int) Math.round(c.getWidth() * progressBar.getPercentComplete());
        if (w > 0) {
            g2.setPaint(new GradientPaint(0, 0, AppTheme.ROSE, w, 0, AppTheme.CRIMSON));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, c.getHeight(), c.getHeight(), c.getHeight()));
        }
        g2.dispose();
    }
}

package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Ascenseur fin, sans flèches, avec curseur arrondi teinté bordeaux. */
public class ThemedScrollBarUI extends BasicScrollBarUI {

    private static final int THICKNESS = 12;

    public static ComponentUI createUI(JComponent c) { return new ThemedScrollBarUI(); }

    @Override protected JButton createDecreaseButton(int o) { return zero(); }
    @Override protected JButton createIncreaseButton(int o) { return zero(); }

    private static JButton zero() {
        JButton b = new JButton();
        Dimension d = new Dimension(0, 0);
        b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
        b.setFocusable(false);
        return b;
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == Adjustable.VERTICAL
            ? new Dimension(THICKNESS, 48) : new Dimension(48, THICKNESS);
    }

    @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { /* piste transparente */ }

    @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        boolean active = isDragging || isThumbRollover();
        g2.setColor(AppTheme.alpha(AppTheme.BURGUNDY, active ? 120 : 60));
        boolean v = scrollbar.getOrientation() == Adjustable.VERTICAL;
        double x = v ? r.x + 3 : r.x, y = v ? r.y : r.y + 3;
        double w = v ? r.width - 6 : r.width, h = v ? r.height : r.height - 6;
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
        g2.dispose();
    }
}

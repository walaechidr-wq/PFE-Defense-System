package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Panneau « carte » : coins arrondis, filet de bordure et ombre très légère.
 * Le remplissage peut être redéfini via {@link #paintFill}.
 */
public class RoundedPanel extends JPanel {

    private final int radius;
    private Color fill;
    private Color line;
    private final boolean shadow;

    public RoundedPanel(LayoutManager lm, int radius, Color fill, Color line, boolean shadow) {
        super(lm);
        this.radius = radius;
        this.fill = fill;
        this.line = line;
        this.shadow = shadow;
        setOpaque(false);
    }

    public void setFill(Color c) { this.fill = c; repaint(); }
    public void setLine(Color c) { this.line = c; repaint(); }

    /** Remplissage du fond ; surchargeable (dégradés, reflets…). */
    protected void paintFill(Graphics2D g2, Shape s, int w, int h) {
        g2.setColor(fill);
        g2.fill(s);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        int w = getWidth(), h = getHeight();
        int bottom = shadow ? 3 : 0;               // réserve pour l'ombre
        if (shadow) {
            for (int i = 0; i < 3; i++) {
                g2.setColor(new Color(138, 13, 30, 7 - i * 2));
                g2.fill(new RoundRectangle2D.Double(i, i + 1, w - i * 2, h - bottom + 2 - i, radius + 2, radius + 2));
            }
        }
        Shape s = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - bottom - 1, radius, radius);
        Graphics2D gf = (Graphics2D) g2.create();
        paintFill(gf, s, w, h - bottom);
        gf.dispose();
        if (line != null) {
            g2.setColor(line);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(s);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Zone défilante à coins arrondis (bordure fine). Affiche un état vide centré
 * lorsque le tableau qu'elle contient n'a aucune ligne.
 */
public class RoundedScrollPane extends JScrollPane {

    private static final int RADIUS = AppTheme.R_MD;
    private final JTable table;
    private String emptyText = "Aucune donnée à afficher";

    public RoundedScrollPane(JTable table) {
        super(table);
        this.table = table;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        getViewport().setBackground(AppTheme.BG_CARD);
        setBackground(AppTheme.BG_CARD);
        getVerticalScrollBar().setUnitIncrement(24);
        getHorizontalScrollBar().setUnitIncrement(24);
        JPanel corner = new JPanel();
        corner.setBackground(TableKit.HEADER_BG);
        setCorner(UPPER_RIGHT_CORNER, corner);
    }

    public void setEmptyText(String t) { this.emptyText = t; repaint(); }

    @Override public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        // fond blanc arrondi + clip pour que l'en-tête et les lignes suivent les coins
        AppTheme.aa(g2);
        Shape round = new RoundRectangle2D.Double(0, 0, w, h, RADIUS * 2, RADIUS * 2);
        g2.setColor(AppTheme.BG_CARD);
        g2.fill(round);
        g2.clip(round);
        super.paint(g2);
        if (table != null && table.getRowCount() == 0) paintEmpty(g2);
        g2.setClip(null);
        g2.setColor(AppTheme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, RADIUS * 2, RADIUS * 2));
        g2.dispose();
    }

    private void paintEmpty(Graphics2D g2) {
        int headH = table.getTableHeader() != null ? table.getTableHeader().getHeight() : 0;
        int w = getWidth(), h = getHeight() - headH;
        int cx = w / 2, cy = headH + h / 2;
        Icon ic = Icons.chip(Icons.INBOX, 48, 24, AppTheme.ROSE_DEEP, AppTheme.tint(AppTheme.SOFT_PINK, .7), true);
        ic.paintIcon(this, g2, cx - 24, cy - 48);
        g2.setFont(AppTheme.F_BODY);
        g2.setColor(AppTheme.TEXT_SECONDARY);
        FontMetrics fm = g2.getFontMetrics();
        String t = emptyText;
        int maxW = Math.max(120, w - 48);
        while (fm.stringWidth(t) > maxW && t.length() > 4) t = t.substring(0, t.length() - 2);
        g2.drawString(t, cx - fm.stringWidth(t) / 2, cy + 18);
    }
}

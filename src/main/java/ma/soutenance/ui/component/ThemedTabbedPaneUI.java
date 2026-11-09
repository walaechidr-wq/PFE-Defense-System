package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Onglets « soulignés » : texte sobre, indicateur crimson sur l'onglet actif,
 * filet de séparation discret. Gère le survol et le focus clavier.
 */
public class ThemedTabbedPaneUI extends BasicTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) { return new ThemedTabbedPaneUI(); }

    private int hover = -1;

    @Override protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(11, 16, 11, 16);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(14, 0, 0, 0);
        tabPane.setOpaque(false);
        tabPane.setFont(AppTheme.F_BODY);
    }

    @Override protected void installListeners() {
        super.installListeners();
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = tabForCoordinate(tabPane, e.getX(), e.getY());
                if (i != hover) { hover = i; tabPane.repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) { hover = -1; tabPane.repaint(); }
        };
        tabPane.addMouseMotionListener(ma);
        tabPane.addMouseListener(ma);
        tabPane.setCursor(Cursor.getDefaultCursor());
    }

    private Font bold() { return AppTheme.F_BOLD; }

    /** Les largeurs sont calculées en gras pour que la sélection ne décale pas les onglets. */
    @Override protected FontMetrics getFontMetrics() { return tabPane.getFontMetrics(bold()); }

    @Override protected int getTabLabelShiftX(int p, int i, boolean sel) { return 0; }
    @Override protected int getTabLabelShiftY(int p, int i, boolean sel) { return 0; }

    @Override protected void paintTabBackground(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
        if (!sel && i == hover) {
            Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
            g2.setColor(AppTheme.BG_HOVER);
            g2.fill(new RoundRectangle2D.Double(x + 2, y + 4, w - 4, h - 8, 10, 10));
            g2.dispose();
        }
    }

    @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
        if (!sel) return;
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        g2.setPaint(new GradientPaint(x, 0, AppTheme.CRIMSON, x + w, 0, AppTheme.ROSE));
        g2.fill(new RoundRectangle2D.Double(x + 10, y + h - 3, w - 20, 3, 3, 3));
        g2.dispose();
    }

    @Override protected void paintText(Graphics g, int p, Font font, FontMetrics m, int i,
                                       String title, Rectangle tr, boolean sel) {
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        boolean en = tabPane.isEnabledAt(i);
        Font f = sel ? bold() : AppTheme.F_BODY;
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(!en ? AppTheme.tint(AppTheme.TEXT_SECONDARY, .5)
                        : sel ? AppTheme.BURGUNDY : (i == hover ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY));
        // centre le texte dans la zone de libellé (largeur calculée en gras)
        int tx = tr.x + (tr.width - fm.stringWidth(title)) / 2;
        g2.drawString(title, tx, tr.y + fm.getAscent() + (tr.height - fm.getHeight()) / 2);
        g2.dispose();
    }

    @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] rects, int i,
                                                 Rectangle iconRect, Rectangle textRect, boolean sel) {
        if (!tabPane.hasFocus() || !sel) return;
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        Rectangle r = rects[i];
        g2.setColor(AppTheme.alpha(AppTheme.CRIMSON, 150));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new RoundRectangle2D.Double(r.x + 3, r.y + 4, r.width - 6, r.height - 9, 10, 10));
        g2.dispose();
    }

    @Override protected void paintContentBorder(Graphics g, int p, int sel) {
        if (p != TOP || tabPane.getTabCount() == 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Rectangle r = tabPane.getBoundsAt(0);
        if (r == null) { g2.dispose(); return; }
        int runs = Math.max(1, runCount);
        int y = calculateTabAreaHeight(p, runs, maxTabHeight) - 1;
        g2.setColor(AppTheme.BORDER);
        g2.drawLine(0, y, tabPane.getWidth(), y);
        g2.dispose();
    }

    @Override protected void paintContentBorderTopEdge(Graphics g, int p, int s, int x, int y, int w, int h) { }
    @Override protected void paintContentBorderLeftEdge(Graphics g, int p, int s, int x, int y, int w, int h) { }
    @Override protected void paintContentBorderRightEdge(Graphics g, int p, int s, int x, int y, int w, int h) { }
    @Override protected void paintContentBorderBottomEdge(Graphics g, int p, int s, int x, int y, int w, int h) { }
}

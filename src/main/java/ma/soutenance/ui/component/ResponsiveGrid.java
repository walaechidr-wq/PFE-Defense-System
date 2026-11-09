package ma.soutenance.ui.component;

import java.awt.*;

/**
 * Grille qui adapte automatiquement le nombre de colonnes à la largeur disponible
 * (cellules d'au moins {@code minCellW} px) et passe à la ligne si besoin.
 * Chaque cellule mesure au moins {@code cellH} px ; avec {@code fill}, elle occupe
 * toute la hauteur disponible lorsque le conteneur est plus grand.
 */
public class ResponsiveGrid implements LayoutManager {

    private final int minCellW, gap, cellH;
    private final boolean fill;

    public ResponsiveGrid(int minCellW, int gap, int cellH, boolean fill) {
        this.minCellW = minCellW; this.gap = gap; this.cellH = cellH; this.fill = fill;
    }

    private int cols(Container p, int width) {
        int n = Math.max(1, p.getComponentCount());
        int c = Math.max(1, (width + gap) / (minCellW + gap));
        return Math.min(c, n);
    }

    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }

    @Override public Dimension preferredLayoutSize(Container p) {
        Insets in = p.getInsets();
        int n = p.getComponentCount();
        if (n == 0) return new Dimension(0, 0);
        int w = p.getWidth() > 0 ? p.getWidth() - in.left - in.right : minCellW * n + gap * (n - 1);
        int c = cols(p, w), rows = (n + c - 1) / c;
        return new Dimension(minCellW, rows * cellH + (rows - 1) * gap + in.top + in.bottom);
    }

    @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(minCellW, cellH); }

    @Override public void layoutContainer(Container p) {
        Insets in = p.getInsets();
        int n = p.getComponentCount();
        if (n == 0) return;
        int w = p.getWidth() - in.left - in.right;
        int c = cols(p, w), rows = (n + c - 1) / c;
        int cw = (w - (c - 1) * gap) / c;
        int ch = cellH;
        if (fill) {
            int avail = (p.getHeight() - in.top - in.bottom - (rows - 1) * gap) / rows;
            ch = Math.max(cellH, avail);
        }
        for (int i = 0; i < n; i++) {
            int r = i / c, col = i % c;
            p.getComponent(i).setBounds(in.left + col * (cw + gap), in.top + r * (ch + gap), cw, ch);
        }
    }
}

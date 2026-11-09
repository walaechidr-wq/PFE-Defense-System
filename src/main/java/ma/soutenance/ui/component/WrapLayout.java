package ma.soutenance.ui.component;

import javax.swing.*;
import java.awt.*;

/** FlowLayout dont la hauteur préférée tient compte du retour à la ligne (barres de boutons adaptatives). */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    private int lastWidth = -1;

    /**
     * La hauteur préférée dépend de la largeur : quand celle-ci change, on invalide le conteneur
     * pour que le parent recalcule sa mise en page (sinon la hauteur reste celle de l'ancienne largeur).
     */
    @Override public void layoutContainer(Container target) {
        super.layoutContainer(target);
        int w = target.getWidth();
        if (w != lastWidth) {
            lastWidth = w;
            SwingUtilities.invokeLater(() -> {
                target.invalidate();
                Container parent = target.getParent();
                if (parent != null) parent.revalidate();
            });
        }
    }

    @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension m = layoutSize(target, false);
        m.width -= getHgap() + 1;
        return m;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Container c = target;
            while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
            int targetWidth = c.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap(), vgap = getVgap();
            Insets in = target.getInsets();
            int extra = in.left + in.right + hgap * 2;
            int maxWidth = targetWidth - extra;

            Dimension dim = new Dimension(0, 0);
            int rowW = 0, rowH = 0;
            for (Component m : target.getComponents()) {
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowW + d.width > maxWidth && rowW > 0) {
                    addRow(dim, rowW, rowH);
                    rowW = 0; rowH = 0;
                }
                if (rowW != 0) rowW += hgap;
                rowW += d.width;
                rowH = Math.max(rowH, d.height);
            }
            addRow(dim, rowW, rowH);
            dim.width += extra;
            dim.height += in.top + in.bottom + vgap * 2;
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, target) != null && target.isValid())
                dim.width -= hgap + 1;
            return dim;
        }
    }

    private void addRow(Dimension dim, int rowW, int rowH) {
        dim.width = Math.max(dim.width, rowW);
        if (dim.height > 0) dim.height += getVgap();
        dim.height += rowH;
    }
}

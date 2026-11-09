package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Style des tableaux : en-têtes teintés, lignes aérées avec filet, survol, sélection rosée,
 * pastilles de statut et « chips » colorées pour les encadrants.
 * Tous les renderers ici sont purement visuels : ils ne modifient jamais le modèle.
 */
public final class TableKit {
    private TableKit() {}

    private static final String THEMED = "themed.table";
    private static final String HOVER  = "themed.hoverRow";

    private static final Color SEL_BG   = new Color(0xFDE4EB);
    private static final Color HAIRLINE = new Color(0xF6E9ED);
    static final Color HEADER_BG = new Color(0xFBF0F3);

    // ── Installation (idempotente) ────────────────────────────────────────────
    public static void style(JTable t) {
        if (Boolean.TRUE.equals(t.getClientProperty(THEMED))) return;
        t.putClientProperty(THEMED, Boolean.TRUE);

        t.setFont(AppTheme.F_BODY);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setShowHorizontalLines(false);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(AppTheme.BG_CARD);
        t.setForeground(AppTheme.TEXT_PRIMARY);
        t.setSelectionBackground(SEL_BG);
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        t.setFillsViewportHeight(true);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new HeaderRenderer());
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 42));

        t.setDefaultRenderer(Object.class, new CellRenderer());
        t.setDefaultRenderer(Number.class, new CellRenderer());
        t.setDefaultRenderer(Integer.class, new CellRenderer());

        // survol de ligne
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHover(t, t.rowAtPoint(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(t, -1); }
        };
        t.addMouseMotionListener(ma);
        t.addMouseListener(ma);
    }

    private static void setHover(JTable t, int row) {
        Object old = t.getClientProperty(HOVER);
        int o = old instanceof Integer i ? i : -1;
        if (o == row) return;
        t.putClientProperty(HOVER, row);
        t.repaint();
    }

    private static Color rowBg(JTable t, int row, boolean sel) {
        if (sel) return SEL_BG;
        Object h = t.getClientProperty(HOVER);
        return (h instanceof Integer i && i == row) ? AppTheme.BG_HOVER : AppTheme.BG_CARD;
    }

    // ── En-tête ───────────────────────────────────────────────────────────────
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setText(v == null ? "" : v.toString());
            setFont(AppTheme.F_BOLD.deriveFont(12f));
            setForeground(AppTheme.BURGUNDY);
            setBackground(HEADER_BG);
            setHorizontalAlignment(alignFor(t, c));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_STRONG),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    private static int alignFor(JTable t, int col) {
        boolean num = Number.class.isAssignableFrom(t.getColumnClass(col))
            || (t.getRowCount() > 0 && t.getValueAt(0, col) instanceof Number);
        return num ? SwingConstants.CENTER : SwingConstants.LEADING;
    }

    // ── Cellule texte ─────────────────────────────────────────────────────────
    public static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setBackground(rowBg(t, r, sel));
            setForeground(AppTheme.TEXT_PRIMARY);
            setHorizontalAlignment(v instanceof Number ? SwingConstants.CENTER : SwingConstants.LEADING);
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(HAIRLINE);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

    // ── Pastilles ─────────────────────────────────────────────────────────────
    /** Retourne {texte, fond} pour un libellé, ou {@code null} pour un texte simple. */
    public interface PillStyle extends Function<String, Color[]> { }

    /** Cellule composée d'une ou plusieurs pastilles (jetons séparés par un espace si {@code split}). */
    public static class PillRenderer extends JPanel implements TableCellRenderer {
        private final PillStyle style;
        private final boolean split;
        private String[] tokens = {};
        private Color rowBg = AppTheme.BG_CARD;
        private Color plain = AppTheme.TEXT_PRIMARY;
        private final IntFunction<Color> chipBg;
        private int row;

        public PillRenderer(PillStyle style, boolean split) { this(style, split, null); }

        /** @param chipBg couleur de fond par ligne (chips encadrants) ; prioritaire sur {@code style} */
        public PillRenderer(PillStyle style, boolean split, IntFunction<Color> chipBg) {
            this.style = style; this.split = split; this.chipBg = chipBg;
            setOpaque(false);
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            String s = v == null ? "" : v.toString().strip();
            tokens = s.isEmpty() ? new String[0] : (split ? s.split("\\s+") : new String[]{s});
            rowBg = rowBg(t, r, sel);
            row = r;
            setFont(t.getFont());
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
            int w = getWidth(), h = getHeight();
            g2.setColor(rowBg); g2.fillRect(0, 0, w, h);
            g2.setColor(HAIRLINE); g2.fillRect(0, h - 1, w, 1);

            Font f = AppTheme.F_SMALL.deriveFont(Font.BOLD);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int x = 14, ph = 24;
            int y = (h - ph) / 2;
            for (String tk : tokens) {
                Color[] cs = null;
                if (chipBg != null) {
                    Color bg = chipBg.apply(row);
                    if (bg != null) cs = new Color[]{AppTheme.TEXT_PRIMARY, bg};
                }
                if (cs == null && style != null) cs = style.apply(tk);
                int tw = fm.stringWidth(tk);
                if (cs == null) {                               // texte simple
                    g2.setFont(getFont()); g2.setColor(plain);
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(tk, x, (h - fm2.getHeight()) / 2 + fm2.getAscent());
                    x += fm2.stringWidth(tk) + 6;
                    g2.setFont(f);
                    continue;
                }
                int pw = Math.min(tw + 22, Math.max(40, w - x - 8));
                g2.setColor(cs[1]);
                g2.fill(new RoundRectangle2D.Double(x, y, pw, ph, ph, ph));
                g2.setColor(cs[0]);
                String shown = clip(tk, fm, pw - 22);
                g2.drawString(shown, x + 11, y + (ph - fm.getHeight()) / 2 + fm.getAscent());
                x += pw + 6;
            }
            g2.dispose();
        }

        private static String clip(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) return s;
            while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
    }

    // ── Styles prêts à l'emploi ───────────────────────────────────────────────
    public static PillStyle langueStyle() {
        return s -> switch (s) {
            case "Anglais"  -> new Color[]{AppTheme.BURGUNDY, AppTheme.tint(AppTheme.ROSE, .78)};
            case "Français" -> new Color[]{AppTheme.TEXT_SECONDARY, new Color(0xF4EEF0)};
            default -> null;
        };
    }

    public static PillStyle conformeStyle() {
        return s -> switch (s) {
            case "Oui" -> new Color[]{AppTheme.SUCCESS, AppTheme.SUCCESS_LIGHT};
            case "Non" -> new Color[]{AppTheme.DANGER, AppTheme.DANGER_LIGHT};
            default -> null;
        };
    }

    public static PillStyle fichiersStyle() {
        return s -> switch (s) {
            case "Excel", "PDF" -> new Color[]{AppTheme.BURGUNDY, AppTheme.tint(AppTheme.SOFT_PINK, .62)};
            case "Aucun" -> new Color[]{AppTheme.TEXT_SECONDARY, new Color(0xF4EEF0)};
            default -> null;
        };
    }
}

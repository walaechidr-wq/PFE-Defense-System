package ma.soutenance.ui.util;

import ma.soutenance.ui.component.RoundedPanel;
import ma.soutenance.ui.component.RoundedScrollPane;
import ma.soutenance.ui.component.TableKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Utilitaires Swing réutilisables dans tous les panneaux. */
public final class SwingUtil {
    private SwingUtil() {}

    // ── Dialogues (habillés globalement par AppTheme.applyLaF) ───────────────
    public static void info(Component p, String titre, String msg)   { JOptionPane.showMessageDialog(p, msg, titre, JOptionPane.INFORMATION_MESSAGE); }
    public static void warn(Component p, String titre, String msg)   { JOptionPane.showMessageDialog(p, msg, titre, JOptionPane.WARNING_MESSAGE); }
    public static void error(Component p, String titre, String msg)  { JOptionPane.showMessageDialog(p, msg, titre, JOptionPane.ERROR_MESSAGE); }
    public static boolean confirm(Component p, String titre, String msg) {
        return JOptionPane.showConfirmDialog(p, msg, titre, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ── Tableaux ──────────────────────────────────────────────────────────────
    /** Applique le style commun (idempotent : peut être appelé plusieurs fois). */
    public static void styleTable(JTable t) { TableKit.style(t); }

    /** Tableau dans une zone défilante arrondie. */
    public static JScrollPane scroll(JTable t) { return scrollPane(t, null); }

    /** Idem, avec un message affiché quand le tableau est vide. */
    public static RoundedScrollPane scrollPane(JTable t, String emptyText) {
        styleTable(t);
        RoundedScrollPane sp = new RoundedScrollPane(t);
        if (emptyText != null) sp.setEmptyText(emptyText);
        return sp;
    }

    /** Applique une colonne de pastilles (langue, conformité, fichiers…). */
    public static void pillColumn(JTable t, int col, TableKit.PillStyle style, boolean split) {
        t.getColumnModel().getColumn(col).setCellRenderer(new TableKit.PillRenderer(style, split));
    }

    // ── Cartes ────────────────────────────────────────────────────────────────
    public static JPanel card(LayoutManager layout) {
        RoundedPanel p = new RoundedPanel(layout, AppTheme.R_MD, AppTheme.BG_CARD, AppTheme.BORDER, true);
        p.setBorder(new EmptyBorder(AppTheme.PAD - 4, AppTheme.PAD - 2, AppTheme.PAD - 1, AppTheme.PAD - 2));
        return p;
    }

    // ── EDT ───────────────────────────────────────────────────────────────────
    public static void onEDT(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // ── Titre de section ──────────────────────────────────────────────────────
    /** Intitulé de section : petit filet d'accent crimson + texte bordeaux. */
    public static JPanel sectionBar(String text) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
                g2.setPaint(new GradientPaint(0, 0, AppTheme.CRIMSON, 0, getHeight(), AppTheme.ROSE));
                int bh = 16;
                g2.fill(new RoundRectangle2D.Double(0, (getHeight() - bh) / 2.0, 4, bh, 4, 4));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 14, 6, 6));
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.F_SECTION);
        l.setForeground(AppTheme.BURGUNDY);
        p.add(l);
        return p;
    }

    // ── En-tête de page ───────────────────────────────────────────────────────
    public static JPanel pageHeader(String title, String subtitle, String iconName, JComponent actions) {
        return pageHeader(title, AppTheme.muted(subtitle), iconName, actions);
    }

    /** @param subtitle label conservé tel quel (les panneaux peuvent continuer à faire {@code setText}). */
    public static JPanel pageHeader(String title, JLabel subtitle, String iconName, JComponent actions) {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, AppTheme.GAP + 2, 0));

        JLabel chip = new JLabel(new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
                g2.setPaint(new GradientPaint(x, y, AppTheme.CRIMSON, x + 46, y + 46, AppTheme.BURGUNDY));
                g2.fill(new RoundRectangle2D.Double(x, y, 46, 46, 15, 15));
                g2.dispose();
                Icons.icon(iconName, 22, Color.WHITE).paintIcon(c, g, x + 12, y + 12);
            }
            @Override public int getIconWidth()  { return 46; }
            @Override public int getIconHeight() { return 46; }
        });
        p.add(chip, BorderLayout.WEST);

        JLabel t = AppTheme.title(title);
        subtitle.setFont(AppTheme.F_BODY);
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 2));
        txt.setOpaque(false);
        txt.add(t);
        txt.add(subtitle);
        p.add(txt, BorderLayout.CENTER);

        if (actions != null) {
            JPanel east = new JPanel(new GridBagLayout());
            east.setOpaque(false);
            east.add(actions);
            p.add(east, BorderLayout.EAST);
        }
        return p;
    }

    /** Barre d'actions alignée à droite (boutons d'export, etc.). */
    public static JPanel actionBar(String label, JComponent... items) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        if (label != null) bar.add(AppTheme.muted(label));
        for (JComponent c : items) bar.add(c);
        return bar;
    }

    // ── État vide ─────────────────────────────────────────────────────────────
    public static JPanel emptyState(String iconName, String title, String description, JButton action) {
        RoundedPanel card = new RoundedPanel(new GridBagLayout(), AppTheme.R_LG, AppTheme.BG_CARD, AppTheme.BORDER, true);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel ic = new JLabel(Icons.chip(iconName, 72, 32, AppTheme.CRIMSON, AppTheme.PRIMARY_LIGHT, true));
        ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(AppTheme.F_SUBTITLE); t.setForeground(AppTheme.TEXT_PRIMARY);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel d = new JLabel("<html><div style='text-align:center;width:340px'>" + description + "</div></html>");
        d.setFont(AppTheme.F_BODY); d.setForeground(AppTheme.TEXT_SECONDARY);
        d.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.add(ic);
        col.add(Box.createVerticalStrut(16));
        col.add(t);
        col.add(Box.createVerticalStrut(6));
        col.add(d);
        if (action != null) {
            col.add(Box.createVerticalStrut(20));
            action.setAlignmentX(Component.CENTER_ALIGNMENT);
            col.add(action);
        }
        card.add(col);
        return card;
    }

    // ── Défilement vertical adaptatif ─────────────────────────────────────────
    /**
     * Enveloppe {@code content} dans un défilement vertical : le contenu suit la largeur de la
     * fenêtre et remplit la hauteur quand il y a de la place, sinon la barre apparaît.
     */
    public static JScrollPane verticalScroll(JComponent content) {
        class Holder extends JPanel implements Scrollable {
            Holder() { super(new BorderLayout()); setOpaque(false); add(content); }
            @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
            @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
            @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(60, r.height - 40); }
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
            @Override public boolean getScrollableTracksViewportHeight() {
                return getParent() instanceof JViewport vp && vp.getHeight() >= getPreferredSize().height;
            }
        }
        JScrollPane sp = new JScrollPane(new Holder(),
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setBackground(AppTheme.BG_APP);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }
}

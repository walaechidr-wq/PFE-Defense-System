package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Barre de navigation latérale — bordeaux profond, entrée active en dégradé crimson.
 * Se réduit à une colonne d'icônes lorsque la fenêtre est étroite ({@link #setCompact}).
 */
public class SidebarPanel extends JPanel {

    public interface NavListener { void onNavigate(String viewId); }

    private final Map<String, NavItem> navBtns = new LinkedHashMap<>();
    private NavListener listener;
    private boolean compact;
    private String activeId;

    private JPanel logo;
    private JLabel logoTitle, logoSub, navCaption, footerText;

    private static final String[][] MENU = {
        {"DASHBOARD",   "Tableau de bord",        Icons.DASHBOARD},
        {"AFFECTATION", "Affectation Encadrants", Icons.USER_CHECK},
        {"PLANNING",    "Planning",               Icons.CALENDAR},
        {"GENERER_PV",  "Générer PVs",            Icons.FILE_TEXT},
        {"CONFORMITE",  "Vérif. Conformité",      Icons.SHIELD_CHECK},
        {"HISTORIQUE",  "Historique",             Icons.HISTORY},
    };

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(AppTheme.SIDEBAR_W, Integer.MAX_VALUE));
        add(buildLogo(),   BorderLayout.NORTH);
        add(buildMenu(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    public void setNavListener(NavListener l) { this.listener = l; }

    public void setActive(String viewId) {
        this.activeId = viewId;
        navBtns.forEach((id, btn) -> { btn.active = id.equals(viewId); btn.repaint(); });
    }

    /** Mode compact : icônes seules. */
    public void setCompact(boolean c) {
        if (c == compact) return;
        compact = c;
        setPreferredSize(new Dimension(c ? AppTheme.SIDEBAR_W_COMPACT : AppTheme.SIDEBAR_W, Integer.MAX_VALUE));
        logoTitle.setVisible(!c); logoSub.setVisible(!c);
        navCaption.setVisible(!c); footerText.setVisible(!c);
        logo.setBorder(new EmptyBorder(24, c ? 14 : 20, 18, c ? 14 : 16));
        navBtns.values().forEach(b -> b.setCompact(c));
        revalidate(); repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        g2.setPaint(new GradientPaint(0, 0, AppTheme.BURGUNDY, 0, getHeight(), AppTheme.BURGUNDY_DARK));
        g2.fillRect(0, 0, getWidth(), getHeight());
        // halo décoratif très discret en haut
        g2.setPaint(new RadialGradientPaint(new Point(getWidth() / 2, 0), getWidth(),
            new float[]{0f, 1f}, new Color[]{new Color(243, 109, 143, 46), new Color(243, 109, 143, 0)}));
        g2.fillRect(0, 0, getWidth(), Math.min(getHeight(), 320));
        g2.dispose();
    }

    // ── Logo ──────────────────────────────────────────────────────────────────
    private JPanel buildLogo() {
        logo = new JPanel(new BorderLayout(12, 0));
        logo.setOpaque(false);
        logo.setBorder(new EmptyBorder(24, 20, 18, 16));

        JLabel mark = new JLabel(new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
                g2.setPaint(new GradientPaint(x, y, AppTheme.ROSE, x + 44, y + 44, AppTheme.CRIMSON));
                g2.fill(new RoundRectangle2D.Double(x, y, 44, 44, 14, 14));
                g2.dispose();
                Icons.icon(Icons.CAP, 24, Color.WHITE).paintIcon(c, g, x + 10, y + 10);
            }
            @Override public int getIconWidth()  { return 44; }
            @Override public int getIconHeight() { return 44; }
        });
        logo.add(mark, BorderLayout.WEST);

        logoTitle = new JLabel("Soutenances PFE");
        logoTitle.setFont(AppTheme.F_SUBTITLE);
        logoTitle.setForeground(Color.WHITE);

        logoSub = new JLabel("Gestion & Planification");
        logoSub.setFont(AppTheme.F_SMALL);
        logoSub.setForeground(AppTheme.TEXT_LIGHT);

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 2));
        txt.setOpaque(false);
        txt.add(logoTitle);
        txt.add(logoSub);
        logo.add(txt, BorderLayout.CENTER);
        return logo;
    }

    // ── Menu ──────────────────────────────────────────────────────────────────
    private JPanel buildMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(4, 12, 0, 12));

        navCaption = new JLabel("NAVIGATION");
        navCaption.setFont(AppTheme.F_SMALL.deriveFont(Font.BOLD, 11f));
        navCaption.setForeground(AppTheme.alpha(AppTheme.TEXT_LIGHT, 170));
        navCaption.setBorder(new EmptyBorder(6, 12, 10, 0));
        navCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(navCaption);

        for (String[] item : MENU) {
            NavItem btn = new NavItem(item[0], item[1], item[2]);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            navBtns.put(item[0], btn);
            menu.add(btn);
            menu.add(Box.createVerticalStrut(4));
        }
        return menu;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 22, 18, 16));
        footerText = new JLabel("ENSA — UAÉ  |  v2.0.0");
        footerText.setFont(AppTheme.F_SMALL);
        footerText.setForeground(AppTheme.alpha(AppTheme.TEXT_LIGHT, 190));
        p.add(footerText, BorderLayout.CENTER);
        return p;
    }

    // ── Entrée de navigation ──────────────────────────────────────────────────
    private final class NavItem extends JButton {
        private final String label, iconName;
        boolean active, hover, compactMode;

        NavItem(String viewId, String label, String iconName) {
            this.label = label; this.iconName = iconName;
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false);
            setFocusPainted(false); setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder());
            setHorizontalAlignment(SwingConstants.LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.NAV_H));
            setPreferredSize(new Dimension(AppTheme.SIDEBAR_W, AppTheme.NAV_H));
            getAccessibleContext().setAccessibleName(label);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
            addActionListener(e -> {
                setActive(viewId);
                if (listener != null) listener.onNavigate(viewId);
            });
        }

        void setCompact(boolean c) {
            compactMode = c;
            setToolTipText(c ? label : null);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
            int w = getWidth(), h = getHeight();
            Shape pill = new RoundRectangle2D.Double(0, 0, w, h, 14, 14);
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, AppTheme.CRIMSON, w, h, AppTheme.mix(AppTheme.CRIMSON, AppTheme.ROSE, .45)));
                g2.fill(pill);
            } else if (hover || isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fill(pill);
            }
            if (isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 13, 13));
            }
            Color fg = active ? Color.WHITE : (hover ? Color.WHITE : new Color(255, 255, 255, 205));
            Icons.VectorIcon ic = Icons.icon(iconName, 20, fg);
            int iy = (h - 20) / 2;
            if (compactMode) {
                ic.paintIcon(this, g2, (w - 20) / 2, iy);
            } else {
                ic.paintIcon(this, g2, 14, iy);
                g2.setFont(active ? AppTheme.F_NAV_SEL : AppTheme.F_NAV);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, 44, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}

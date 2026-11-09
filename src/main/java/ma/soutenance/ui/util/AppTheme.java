package ma.soutenance.ui.util;

import ma.soutenance.ui.component.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Système de design centralisé — identité « Burgundy & Rose ».
 *
 * <p>Tous les jetons (couleurs, polices, rayons, dimensions) vivent ici, ainsi que
 * les fabriques de composants (boutons, labels, cartes KPI, badges) et
 * l'installation du Look &amp; Feel. Les noms publics historiques sont conservés :
 * aucun panneau existant n'a besoin d'être réécrit pour compiler.</p>
 */
public final class AppTheme {
    private AppTheme() {}

    // ── Palette de marque ─────────────────────────────────────────────────────
    public static final Color BURGUNDY   = new Color(0x8A0D1E);   // marque, navigation, profondeur
    public static final Color CRIMSON    = new Color(0xDE2E4B);   // interactions, états actifs
    public static final Color ROSE       = new Color(0xF36D8F);   // accents secondaires, graphiques
    public static final Color SOFT_PINK  = new Color(0xF59FBD);   // détails subtils, fonds doux

    // Dérivés (profondeur / lisibilité) — jamais utilisés comme couleur de marque principale
    public static final Color BURGUNDY_DARK = new Color(0x5C0713);
    public static final Color CRIMSON_DEEP  = new Color(0xC0213F); // fin de dégradé (contraste texte blanc ≥ 5:1)
    public static final Color ROSE_DEEP     = new Color(0xC93B66); // rose lisible sur fond clair (icônes, textes)

    // Alias historiques → nouvelle palette
    public static final Color PRIMARY        = CRIMSON;
    public static final Color PRIMARY_DARK   = BURGUNDY;
    public static final Color PRIMARY_LIGHT  = new Color(0xFDE9EE);
    public static final Color ACCENT         = ROSE;

    // Sémantique (statuts)
    public static final Color DANGER         = new Color(0xC62828);
    public static final Color DANGER_LIGHT   = new Color(0xFDECEC);
    public static final Color WARNING        = new Color(0xA86512);
    public static final Color WARNING_LIGHT  = new Color(0xFFF3DC);
    public static final Color SUCCESS        = new Color(0x187A4D);
    public static final Color SUCCESS_LIGHT  = new Color(0xE3F5EB);

    // Surfaces
    public static final Color BG_APP         = new Color(0xFFF8FA);
    public static final Color BG_CARD        = Color.WHITE;
    public static final Color BG_SIDEBAR     = new Color(0x6E0A19);
    public static final Color BG_SIDEBAR_HOV = new Color(0x7D0E20);
    public static final Color BG_TABLE_ALT   = new Color(0xFFF6F8);
    public static final Color BG_TABLE_EN    = new Color(0xFFF3DC);
    public static final Color BG_HOVER       = new Color(0xFFF3F6);
    public static final Color BORDER         = new Color(0xF0DDE2);
    public static final Color BORDER_STRONG  = new Color(0xE6C9D1);

    // Texte
    public static final Color TEXT_PRIMARY   = new Color(0x252126);
    public static final Color TEXT_SECONDARY = new Color(0x71666B);
    public static final Color TEXT_LIGHT     = new Color(0xF7C9D6);   // sur fond bordeaux
    public static final Color TEXT_WHITE     = Color.WHITE;

    // Palette des graphiques (ordre d'utilisation)
    public static final Color[] CHART_COLORS = {
        BURGUNDY, CRIMSON, ROSE, SOFT_PINK,
        new Color(0xB3173A), new Color(0xFBC8D8), BURGUNDY_DARK, new Color(0xC77A8D)
    };

    // ── Typographie ───────────────────────────────────────────────────────────
    private static final String FAMILY = resolveFamily();

    public static final Font F_TITLE    = new Font(FAMILY, Font.BOLD,  22);
    public static final Font F_SUBTITLE = new Font(FAMILY, Font.BOLD,  15);
    public static final Font F_SECTION  = new Font(FAMILY, Font.BOLD,  14);
    public static final Font F_BODY     = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font F_SMALL    = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font F_BOLD     = new Font(FAMILY, Font.BOLD,  13);
    public static final Font F_MONO     = new Font("Consolas", Font.PLAIN, 12);
    public static final Font F_KPI      = new Font(FAMILY, Font.BOLD,  32);
    public static final Font F_NAV      = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font F_NAV_SEL  = new Font(FAMILY, Font.BOLD,  13);

    private static String resolveFamily() {
        String[] pref = {"Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Arial"};
        try {
            Set<String> av = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String f : pref) if (av.contains(f)) return f;
        } catch (Throwable ignored) { }
        return Font.SANS_SERIF;
    }

    // ── Dimensions & rayons ───────────────────────────────────────────────────
    public static final int SIDEBAR_W         = 248;
    public static final int SIDEBAR_W_COMPACT = 76;
    public static final int HEADER_H          = 64;
    public static final int STATUS_H          = 32;
    public static final int NAV_H             = 44;
    public static final int PAD               = 22;
    public static final int GAP               = 14;
    public static final int CONTROL_H         = 38;

    public static final int R_SM = 8;
    public static final int R_MD = 14;
    public static final int R_LG = 20;

    // ── Utilitaires couleur / rendu ───────────────────────────────────────────
    /** Mélange {@code c} avec du blanc : amt=0 → c, amt=1 → blanc. */
    public static Color tint(Color c, double amt) { return mix(c, Color.WHITE, amt); }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    /** Active le lissage des formes et du texte. */
    public static Graphics2D aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    // ── Fabrique de boutons (variantes gérées par ThemedButtonUI) ─────────────
    public static final String VARIANT = "variant";

    private static JButton button(String text, String variant, String icon) {
        JButton b = new JButton(text);
        b.putClientProperty(VARIANT, variant);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (icon != null) {
            Color c = switch (variant) {
                case "primary", "success" -> Color.WHITE;
                case "danger" -> DANGER;
                default -> BURGUNDY;
            };
            b.setIcon(Icons.icon(icon, 16, c));
        }
        return b;
    }

    public static JButton btnPrimary(String text)   { return button(text, "primary", null); }
    public static JButton btnSecondary(String text) { return button(text, "secondary", null); }
    public static JButton btnDanger(String text)    { return button(text, "danger", null); }
    public static JButton btnSuccess(String text)   { return button(text, "success", null); }

    public static JButton btnPrimary(String text, String icon)   { return button(text, "primary", icon); }
    public static JButton btnSecondary(String text, String icon) { return button(text, "secondary", icon); }
    public static JButton btnDanger(String text, String icon)    { return button(text, "danger", icon); }

    // ── Fabrique de labels ────────────────────────────────────────────────────
    public static JLabel title(String t)   { JLabel l = new JLabel(t); l.setFont(F_TITLE);   l.setForeground(TEXT_PRIMARY);   return l; }
    public static JLabel section(String t) { JLabel l = new JLabel(t); l.setFont(F_SECTION); l.setForeground(BURGUNDY);       return l; }
    public static JLabel body(String t)    { JLabel l = new JLabel(t); l.setFont(F_BODY);    l.setForeground(TEXT_PRIMARY);   return l; }
    public static JLabel muted(String t)   { JLabel l = new JLabel(t); l.setFont(F_SMALL);   l.setForeground(TEXT_SECONDARY); return l; }

    // ── Cartes KPI ────────────────────────────────────────────────────────────
    /** Carte KPI standard : pastille d'icône teintée, valeur, libellé. */
    public static JPanel kpiCard(String valeur, String label, Color color) {
        return kpiCard(valeur, label, color, null);
    }

    public static JPanel kpiCard(String valeur, String label, Color color, String iconName) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 10), R_MD, BG_CARD, BORDER, true);
        p.setBorder(new EmptyBorder(16, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel ll = new JLabel(label);
        ll.setFont(F_BOLD); ll.setForeground(TEXT_SECONDARY);
        top.add(ll, BorderLayout.CENTER);
        if (iconName != null) {
            top.add(new JLabel(Icons.chip(iconName, 36, 18, color, tint(color, 0.88), false)), BorderLayout.EAST);
        }

        JLabel vl = new JLabel(valeur);
        vl.setFont(F_KPI); vl.setForeground(TEXT_PRIMARY);

        // filet d'accent discret sous la valeur
        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa((Graphics2D) g.create());
                g2.setPaint(new GradientPaint(0, 0, color, 44, 0, alpha(color, 40)));
                g2.fill(new RoundRectangle2D.Double(0, 0, 44, 4, 4, 4));
                g2.dispose();
            }
        };
        accent.setOpaque(false);
        accent.setPreferredSize(new Dimension(44, 4));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(vl, BorderLayout.CENTER);
        JPanel accentWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        accentWrap.setOpaque(false);
        accentWrap.add(accent);
        bottom.add(accentWrap, BorderLayout.SOUTH);

        p.add(top, BorderLayout.NORTH);
        p.add(bottom, BorderLayout.CENTER);
        return p;
    }

    /** Carte KPI « à la une » : dégradé bordeaux → crimson, texte blanc. */
    public static JPanel kpiHero(String valeur, String label, String iconName) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 10), R_MD, BURGUNDY, null, true) {
            @Override protected void paintFill(Graphics2D g2, Shape s, int w, int h) {
                g2.setPaint(new GradientPaint(0, 0, BURGUNDY, w, h, CRIMSON));
                g2.fill(s);
                // reflet décoratif discret
                g2.setClip(s);
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(w - 110, -70, 190, 190);
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(w - 60, h - 60, 130, 130);
            }
        };
        p.setBorder(new EmptyBorder(16, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel ll = new JLabel(label);
        ll.setFont(F_BOLD); ll.setForeground(new Color(255, 255, 255, 220));
        top.add(ll, BorderLayout.CENTER);
        if (iconName != null)
            top.add(new JLabel(Icons.chip(iconName, 36, 18, Color.WHITE, new Color(255, 255, 255, 40), false)),
                    BorderLayout.EAST);

        JLabel vl = new JLabel(valeur);
        vl.setFont(F_KPI); vl.setForeground(Color.WHITE);

        p.add(top, BorderLayout.NORTH);
        p.add(vl, BorderLayout.CENTER);
        return p;
    }

    // ── Badges ────────────────────────────────────────────────────────────────
    /** Pastille arrondie (statut, langue…). */
    public static JLabel badge(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa((Graphics2D) g.create());
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(F_SMALL.deriveFont(Font.BOLD));
        l.setForeground(fg); l.setBackground(bg); l.setOpaque(false);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        return l;
    }

    public static JLabel okBadge()  { return badge("Conforme", SUCCESS, SUCCESS_LIGHT); }
    public static JLabel errBadge() { return badge("Erreurs",  DANGER,  DANGER_LIGHT); }

    // ── Look & Feel ───────────────────────────────────────────────────────────
    public static void applyLaF() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 1) Teinte de base de Nimbus (avant l'installation du L&F)
        UIManager.put("nimbusBase",              BURGUNDY);
        UIManager.put("nimbusBlueGrey",          new Color(0xB89AA3));
        UIManager.put("control",                 Color.WHITE);
        UIManager.put("nimbusLightBackground",   Color.WHITE);
        UIManager.put("nimbusSelectionBackground", CRIMSON);
        UIManager.put("nimbusFocus",             CRIMSON);
        UIManager.put("nimbusOrange",            ROSE);
        UIManager.put("text",                    TEXT_PRIMARY);
        UIManager.put("info",                    BURGUNDY_DARK);

        try {
            for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(laf.getName())) {
                    UIManager.setLookAndFeel(laf.getClassName()); break;
                }
            }
        } catch (Exception ignored) {}

        // 2) Délégués d'interface sur mesure (boutons, ascenseurs, onglets, progression)
        UIManager.put("ButtonUI",      "ma.soutenance.ui.component.ThemedButtonUI");
        UIManager.put("ScrollBarUI",   "ma.soutenance.ui.component.ThemedScrollBarUI");
        UIManager.put("TabbedPaneUI",  "ma.soutenance.ui.component.ThemedTabbedPaneUI");
        UIManager.put("ProgressBarUI", "ma.soutenance.ui.component.ThemedProgressBarUI");

        // 3) Polices
        for (String k : new String[]{"Label.font", "Button.font", "TextField.font", "ComboBox.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TabbedPane.font", "List.font",
                "FormattedTextField.font", "PasswordField.font", "TextArea.font", "ToolTip.font",
                "CheckBox.font", "RadioButton.font", "Spinner.font", "Menu.font", "MenuItem.font"})
            UIManager.put(k, new FontUIResource(F_BODY));
        UIManager.put("Table.font",       new FontUIResource(F_BODY));
        UIManager.put("TableHeader.font", new FontUIResource(F_BOLD));
        UIManager.put("ToolTip.font",     new FontUIResource(F_SMALL));

        // 4) Couleurs & métriques
        UIManager.put("Table.rowHeight",         38);
        UIManager.put("Table.gridColor",         BORDER);
        UIManager.put("Table.intercellSpacing",  new Dimension(0, 0));
        UIManager.put("OptionPane.background",   Color.WHITE);
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("OptionPane.border",       new EmptyBorder(22, 24, 18, 24));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(14, 0, 0, 0));
        UIManager.put("OptionPane.sameSizeButtons", Boolean.FALSE);
        UIManager.put("ToolTip.background",      new ColorUIResource(BURGUNDY_DARK));
        UIManager.put("ToolTip.foreground",      new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border",          new EmptyBorder(6, 10, 6, 10));

        // 5) Icônes vectorielles pour les boîtes de dialogue (plus d'icônes système).
        //    Nimbus définit ces clés dans ses propres valeurs par défaut : on les surcharge à ce niveau.
        UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
        Icon info = Icons.chip(Icons.INFO,           44, 22, BURGUNDY, tint(CRIMSON, 0.88), true);
        Icon ask  = Icons.chip(Icons.HELP,           44, 22, BURGUNDY, tint(CRIMSON, 0.88), true);
        Icon warn = Icons.chip(Icons.ALERT_TRIANGLE, 44, 22, WARNING,  WARNING_LIGHT, true);
        Icon err  = Icons.chip(Icons.ALERT_CIRCLE,   44, 22, DANGER,   DANGER_LIGHT, true);
        for (UIDefaults d : new UIDefaults[]{lafDefaults, UIManager.getDefaults()}) {
            d.put("OptionPane.informationIcon", info);
            d.put("OptionPane.questionIcon",    ask);
            d.put("OptionPane.warningIcon",     warn);
            d.put("OptionPane.errorIcon",       err);
        }

        // 6) Libellés des boutons de dialogue en français (indépendant de la langue du système)
        UIManager.put("OptionPane.yesButtonText",    "Oui");
        UIManager.put("OptionPane.noButtonText",     "Non");
        UIManager.put("OptionPane.okButtonText",     "OK");
        UIManager.put("OptionPane.cancelButtonText", "Annuler");

        ToolTipManager.sharedInstance().setInitialDelay(350);
    }
}

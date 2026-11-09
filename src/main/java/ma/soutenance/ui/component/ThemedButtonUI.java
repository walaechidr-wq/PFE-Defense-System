package ma.soutenance.ui.component;

import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Délégué d'interface des boutons — installé globalement par {@link AppTheme#applyLaF()}.
 *
 * Variantes (propriété client « variant ») : primary · secondary · danger · success · ghost.
 * Sans propriété, le bouton par défaut d'un dialogue devient « primary », les autres « secondary ».
 */
public class ThemedButtonUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent c) { return new ThemedButtonUI(); }

    private static final int RADIUS = 10;

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setIconTextGap(8);
        b.setFont(AppTheme.F_BOLD);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static String variantOf(AbstractButton b) {
        Object v = b.getClientProperty(AppTheme.VARIANT);
        if (v instanceof String s) return s;
        JRootPane rp = SwingUtilities.getRootPane(b);
        if (rp != null && rp.getDefaultButton() == b) return "primary";
        return "secondary";
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        FontMetrics fm = b.getFontMetrics(b.getFont());
        String t = b.getText();
        int w = 36;
        if (b.getIcon() != null) w += b.getIcon().getIconWidth() + (t != null && !t.isEmpty() ? b.getIconTextGap() : 0);
        if (t != null) w += fm.stringWidth(t);
        return new Dimension(Math.max(w, 44), AppTheme.CONTROL_H);
    }

    @Override public Dimension getMinimumSize(JComponent c)  { return getPreferredSize(c); }

    @Override public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        String v = variantOf(b);
        boolean enabled = b.isEnabled();
        boolean hover = enabled && (m.isRollover() || m.isArmed());
        boolean pressed = enabled && m.isPressed() && m.isArmed();

        int w = c.getWidth(), h = c.getHeight();
        Graphics2D g2 = AppTheme.aa((Graphics2D) g.create());
        RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, RADIUS * 2, RADIUS * 2);

        Color fg; Color border = null;
        switch (v) {
            case "primary", "success" -> {
                Color a = v.equals("success") ? AppTheme.SUCCESS : AppTheme.CRIMSON;
                Color z = v.equals("success") ? AppTheme.mix(AppTheme.SUCCESS, Color.BLACK, .18) : AppTheme.CRIMSON_DEEP;
                if (!enabled)      { g2.setColor(AppTheme.tint(AppTheme.CRIMSON, .78)); }
                else {
                    if (pressed)   { a = AppTheme.mix(a, Color.BLACK, .16); z = AppTheme.mix(z, Color.BLACK, .16); }
                    else if (hover){ a = AppTheme.mix(a, Color.WHITE, .08); z = AppTheme.mix(z, Color.WHITE, .08); }
                    g2.setPaint(new GradientPaint(0, 0, a, w, h, z));
                }
                g2.fill(shape);
                fg = Color.WHITE;
            }
            case "danger" -> {
                g2.setColor(!enabled ? Color.WHITE : pressed ? AppTheme.mix(AppTheme.DANGER_LIGHT, AppTheme.DANGER, .12)
                                      : hover ? AppTheme.DANGER_LIGHT : Color.WHITE);
                g2.fill(shape);
                border = enabled ? AppTheme.mix(AppTheme.DANGER, Color.WHITE, .55) : AppTheme.BORDER;
                fg = enabled ? AppTheme.DANGER : AppTheme.tint(AppTheme.TEXT_SECONDARY, .5);
            }
            case "ghost" -> {
                if (hover) { g2.setColor(AppTheme.PRIMARY_LIGHT); g2.fill(shape); }
                fg = enabled ? AppTheme.TEXT_SECONDARY : AppTheme.tint(AppTheme.TEXT_SECONDARY, .5);
            }
            default -> { // secondary
                g2.setColor(!enabled ? AppTheme.BG_APP : pressed ? AppTheme.PRIMARY_LIGHT
                                      : hover ? AppTheme.BG_HOVER : Color.WHITE);
                g2.fill(shape);
                border = !enabled ? AppTheme.BORDER : hover ? AppTheme.ROSE : AppTheme.BORDER_STRONG;
                fg = enabled ? AppTheme.BURGUNDY : AppTheme.tint(AppTheme.TEXT_SECONDARY, .5);
            }
        }
        if (border != null) { g2.setColor(border); g2.setStroke(new BasicStroke(1f)); g2.draw(shape); }

        // Contenu : icône + texte centrés
        g2.setFont(b.getFont());
        FontMetrics fm = g2.getFontMetrics();
        String t = b.getText() == null ? "" : b.getText();
        Icon ic = b.getIcon();
        int tw = fm.stringWidth(t);
        int iw = ic != null ? ic.getIconWidth() : 0;
        int gap = (ic != null && !t.isEmpty()) ? b.getIconTextGap() : 0;
        int total = iw + gap + tw;
        int x = (w - total) / 2;
        if (ic != null) {
            int iy = (h - ic.getIconHeight()) / 2;
            if (ic instanceof Icons.VectorIcon vi) vi.paintWith(g2, x, iy, fg);
            else ic.paintIcon(b, g2, x, iy);
            x += iw + gap;
        }
        g2.setColor(fg);
        g2.drawString(t, x, (h - fm.getHeight()) / 2 + fm.getAscent());

        // Indicateur de focus clavier (accessibilité)
        if (b.isFocusOwner() && enabled) {
            g2.setColor(AppTheme.alpha(AppTheme.CRIMSON, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, RADIUS * 2, RADIUS * 2));
        }
        g2.dispose();
    }
}

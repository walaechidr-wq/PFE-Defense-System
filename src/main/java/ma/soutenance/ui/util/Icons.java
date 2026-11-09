package ma.soutenance.ui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Jeu d'icônes vectorielles (trait fin, grille 24×24) dessinées avec Java2D.
 * Remplace les emojis utilisés auparavant comme icônes d'interface et
 * n'ajoute aucune dépendance au projet.
 */
public final class Icons {
    private Icons() {}

    public static final String DASHBOARD = "dashboard";
    public static final String USER_CHECK = "user-check";
    public static final String USERS = "users";
    public static final String CALENDAR = "calendar";
    public static final String FILE_TEXT = "file-text";
    public static final String SHIELD_CHECK = "shield-check";
    public static final String HISTORY = "history";
    public static final String UPLOAD = "upload";
    public static final String CAP = "cap";
    public static final String TABLE = "table";
    public static final String TRASH = "trash";
    public static final String FOLDER = "folder";
    public static final String REFRESH = "refresh";
    public static final String CHECK_CIRCLE = "check-circle";
    public static final String ALERT_CIRCLE = "alert-circle";
    public static final String ALERT_TRIANGLE = "alert-triangle";
    public static final String INFO = "info";
    public static final String HELP = "help";
    public static final String GLOBE = "globe";
    public static final String LINK = "link";
    public static final String PLAY = "play";
    public static final String LAYERS = "layers";
    public static final String INBOX = "inbox";
    public static final String CLOCK = "clock";
    public static final String XCIRCLE = "x-circle";

    private static final Map<String, Shape> SHAPES = new HashMap<>();

    // ── Fabriques publiques ───────────────────────────────────────────────────

    /** Icône trait de taille {@code size} px et de couleur {@code color}. */
    public static VectorIcon icon(String name, int size, Color color) {
        return new VectorIcon(name, size, color, 1.8f);
    }

    /** Icône posée sur une pastille arrondie colorée (cartes KPI, dialogues). */
    public static Icon chip(String name, int box, int glyph, Color fg, Color bg, boolean round) {
        VectorIcon g = new VectorIcon(name, glyph, fg, 1.8f);
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g2 = (Graphics2D) g0.create();
                AppTheme.aa(g2);
                g2.setColor(bg);
                if (round) g2.fill(new Ellipse2D.Double(x, y, box, box));
                else g2.fill(new RoundRectangle2D.Double(x, y, box, box, box * 0.32, box * 0.32));
                g2.dispose();
                g.paintIcon(c, g0, x + (box - glyph) / 2, y + (box - glyph) / 2);
            }
            @Override public int getIconWidth()  { return box; }
            @Override public int getIconHeight() { return box; }
        };
    }

    /** Image carrée avec dégradé de marque + coiffe : icône de fenêtre. */
    public static Image appImage(int px) {
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        AppTheme.aa(g);
        g.setPaint(new GradientPaint(0, 0, AppTheme.CRIMSON, px, px, AppTheme.BURGUNDY));
        g.fill(new RoundRectangle2D.Double(0, 0, px, px, px * 0.28, px * 0.28));
        int s = (int) (px * 0.62);
        icon(CAP, s, Color.WHITE).paintIcon(null, g, (px - s) / 2, (px - s) / 2);
        g.dispose();
        return img;
    }

    // ── Icône vectorielle ─────────────────────────────────────────────────────

    public static final class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;
        private final float stroke;

        VectorIcon(String name, int size, Color color, float stroke) {
            this.name = name; this.size = size; this.color = color; this.stroke = stroke;
        }

        public VectorIcon withColor(Color c) { return new VectorIcon(name, size, c, stroke); }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            paintWith(g, x, y, color);
        }

        public void paintWith(Graphics g, int x, int y, Color col) {
            Graphics2D g2 = (Graphics2D) g.create();
            AppTheme.aa(g2);
            g2.translate(x, y);
            double sc = size / 24.0;
            g2.scale(sc, sc);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(col);
            g2.draw(shape(name));
            g2.dispose();
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // ── Géométrie des icônes (grille 24×24) ───────────────────────────────────

    private static synchronized Shape shape(String n) {
        return SHAPES.computeIfAbsent(n, Icons::build);
    }

    private static Path2D.Double P() { return new Path2D.Double(); }

    private static void line(Path2D p, double x1, double y1, double x2, double y2) {
        p.moveTo(x1, y1); p.lineTo(x2, y2);
    }
    private static void dot(Path2D p, double x, double y) {
        p.moveTo(x, y); p.lineTo(x, y + 0.01);
    }
    private static void poly(Path2D p, double... c) {
        p.moveTo(c[0], c[1]);
        for (int i = 2; i < c.length; i += 2) p.lineTo(c[i], c[i + 1]);
    }
    private static void circle(Path2D p, double cx, double cy, double r) {
        p.append(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), false);
    }
    private static void rrect(Path2D p, double x, double y, double w, double h, double r) {
        p.append(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2), false);
    }

    private static Shape build(String n) {
        Path2D.Double p = P();
        switch (n) {
            case DASHBOARD -> {
                rrect(p, 3, 3, 7.5, 9.5, 1.6); rrect(p, 13.5, 3, 7.5, 5.5, 1.6);
                rrect(p, 13.5, 12, 7.5, 9, 1.6); rrect(p, 3, 16, 7.5, 5, 1.6);
            }
            case USER_CHECK -> {
                circle(p, 9, 7.5, 3.8);
                p.moveTo(2.5, 21); p.lineTo(2.5, 19.5);
                p.curveTo(2.5, 17, 4.5, 15, 7, 15); p.lineTo(11, 15);
                p.curveTo(13.5, 15, 15.5, 17, 15.5, 19.5); p.lineTo(15.5, 21);
                poly(p, 16.5, 11, 18.5, 13, 22, 9);
            }
            case USERS -> {
                circle(p, 9, 7.5, 3.6);
                p.moveTo(2.5, 20.5); p.lineTo(2.5, 19);
                p.curveTo(2.5, 16.8, 4.3, 15, 6.5, 15); p.lineTo(11.5, 15);
                p.curveTo(13.7, 15, 15.5, 16.8, 15.5, 19); p.lineTo(15.5, 20.5);
                p.moveTo(16, 3.6); p.curveTo(18.8, 4.2, 18.8, 10.6, 16, 11.2);
                p.moveTo(18.5, 15.2); p.curveTo(20.6, 15.8, 21.5, 17.4, 21.5, 19); p.lineTo(21.5, 20.5);
            }
            case CALENDAR -> {
                rrect(p, 3, 4.5, 18, 16.5, 2.2); line(p, 3, 10, 21, 10);
                line(p, 8, 2.5, 8, 6.5); line(p, 16, 2.5, 16, 6.5);
            }
            case FILE_TEXT -> {
                p.moveTo(14, 3); p.lineTo(7, 3);
                p.curveTo(5.9, 3, 5, 3.9, 5, 5); p.lineTo(5, 19);
                p.curveTo(5, 20.1, 5.9, 21, 7, 21); p.lineTo(17, 21);
                p.curveTo(18.1, 21, 19, 20.1, 19, 19); p.lineTo(19, 8); p.closePath();
                poly(p, 14, 3, 14, 8, 19, 8);
                line(p, 9, 13, 15, 13); line(p, 9, 17, 15, 17);
            }
            case SHIELD_CHECK -> {
                p.moveTo(12, 2.8); p.lineTo(19.5, 5.8); p.lineTo(19.5, 11);
                p.curveTo(19.5, 16, 16.4, 19.6, 12, 21.2);
                p.curveTo(7.6, 19.6, 4.5, 16, 4.5, 11); p.lineTo(4.5, 5.8); p.closePath();
                poly(p, 8.6, 12, 11, 14.4, 15.6, 9.6);
            }
            case HISTORY, CLOCK -> {
                circle(p, 12, 12, 9); poly(p, 12, 6.8, 12, 12, 15.6, 14.2);
            }
            case UPLOAD -> {
                line(p, 12, 15, 12, 4); poly(p, 7.5, 8.5, 12, 4, 16.5, 8.5);
                p.moveTo(3.5, 15); p.lineTo(3.5, 18.5);
                p.curveTo(3.5, 19.9, 4.6, 21, 6, 21); p.lineTo(18, 21);
                p.curveTo(19.4, 21, 20.5, 19.9, 20.5, 18.5); p.lineTo(20.5, 15);
            }
            case CAP -> {
                poly(p, 12, 4, 1.8, 9.2, 12, 14.4, 22.2, 9.2); p.closePath();
                p.moveTo(6, 11.8); p.lineTo(6, 16.2);
                p.curveTo(6, 17.8, 8.7, 19.2, 12, 19.2);
                p.curveTo(15.3, 19.2, 18, 17.8, 18, 16.2); p.lineTo(18, 11.8);
                line(p, 22.2, 9.2, 22.2, 15);
            }
            case TABLE -> {
                rrect(p, 3, 4, 18, 16, 2.2); line(p, 3, 9.5, 21, 9.5);
                line(p, 3, 14.8, 21, 14.8); line(p, 9.5, 9.5, 9.5, 20);
            }
            case TRASH -> {
                line(p, 3.5, 6.5, 20.5, 6.5); poly(p, 9, 6.5, 9, 3.8, 15, 3.8, 15, 6.5);
                p.moveTo(5.8, 6.5); p.lineTo(6.8, 19.5);
                p.curveTo(6.9, 20.4, 7.6, 21, 8.5, 21); p.lineTo(15.5, 21);
                p.curveTo(16.4, 21, 17.1, 20.4, 17.2, 19.5); p.lineTo(18.2, 6.5);
                line(p, 10, 10.5, 10, 16.5); line(p, 14, 10.5, 14, 16.5);
            }
            case FOLDER -> {
                p.moveTo(3, 7); p.curveTo(3, 5.9, 3.9, 5, 5, 5); p.lineTo(9, 5); p.lineTo(11.2, 7.4);
                p.lineTo(19, 7.4); p.curveTo(20.1, 7.4, 21, 8.3, 21, 9.4); p.lineTo(21, 17.4);
                p.curveTo(21, 18.5, 20.1, 19.4, 19, 19.4); p.lineTo(5, 19.4);
                p.curveTo(3.9, 19.4, 3, 18.5, 3, 17.4); p.closePath();
            }
            case REFRESH -> {
                p.append(new Arc2D.Double(4, 4, 16, 16, 35, 290, Arc2D.OPEN), false);
                poly(p, 19.6, 3.6, 19.6, 8.6, 14.6, 8.6);
            }
            case CHECK_CIRCLE -> { circle(p, 12, 12, 9); poly(p, 7.8, 12.4, 10.7, 15.3, 16.4, 9.2); }
            case XCIRCLE -> {
                circle(p, 12, 12, 9); line(p, 9, 9, 15, 15); line(p, 15, 9, 9, 15);
            }
            case ALERT_CIRCLE -> {
                circle(p, 12, 12, 9); line(p, 12, 7.5, 12, 12.8); dot(p, 12, 16.4);
            }
            case ALERT_TRIANGLE -> {
                p.moveTo(10.3, 4.4); p.curveTo(11, 3.2, 13, 3.2, 13.7, 4.4); p.lineTo(21.6, 18);
                p.curveTo(22.3, 19.2, 21.4, 20.6, 20, 20.6); p.lineTo(4, 20.6);
                p.curveTo(2.6, 20.6, 1.7, 19.2, 2.4, 18); p.closePath();
                line(p, 12, 9.6, 12, 14); dot(p, 12, 17.2);
            }
            case INFO -> { circle(p, 12, 12, 9); line(p, 12, 11, 12, 16.4); dot(p, 12, 7.8); }
            case HELP -> {
                circle(p, 12, 12, 9);
                p.moveTo(9.3, 9.6); p.curveTo(9.3, 8, 10.5, 7, 12, 7);
                p.curveTo(13.5, 7, 14.7, 8, 14.7, 9.5);
                p.curveTo(14.7, 11.4, 12, 11.4, 12, 13.6); dot(p, 12, 16.9);
            }
            case GLOBE -> {
                circle(p, 12, 12, 9); line(p, 3, 12, 21, 12);
                p.moveTo(12, 3); p.curveTo(8.6, 6.6, 8.6, 17.4, 12, 21);
                p.moveTo(12, 3); p.curveTo(15.4, 6.6, 15.4, 17.4, 12, 21);
            }
            case LINK -> { circle(p, 8.8, 12, 5.2); circle(p, 15.2, 12, 5.2); }
            case PLAY -> { poly(p, 7.5, 4.8, 19, 12, 7.5, 19.2); p.closePath(); }
            case LAYERS -> {
                poly(p, 12, 3, 21.5, 8, 12, 13, 2.5, 8); p.closePath();
                poly(p, 2.5, 12.2, 12, 17.2, 21.5, 12.2); poly(p, 2.5, 16.4, 12, 21.4, 21.5, 16.4);
            }
            case INBOX -> {
                poly(p, 3, 13, 6.2, 5.2, 17.8, 5.2, 21, 13, 21, 18); p.lineTo(21, 18);
                p.curveTo(21, 19.1, 20.1, 20, 19, 20); p.lineTo(5, 20);
                p.curveTo(3.9, 20, 3, 19.1, 3, 18); p.closePath();
                poly(p, 3, 13, 8.2, 13, 9.6, 15.6, 14.4, 15.6, 15.8, 13, 21, 13);
            }
            default -> circle(p, 12, 12, 8);
        }
        return p;
    }
}

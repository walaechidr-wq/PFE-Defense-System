package ma.soutenance.ui.util;

import ma.soutenance.model.Professeur;
import java.awt.Color;
import java.util.*;

/**
 * Registre centralisé de couleurs pour les professeurs.
 *
 * Responsabilité unique : associer une couleur unique et reproductible
 * à chaque professeur. Utilisé par l'UI, l'export Excel et l'export PDF.
 *
 * Algorithme : palette de 20 couleurs pastel prédéfinies + hash du nom
 * pour sélectionner de façon déterministe (même couleur à chaque run).
 */
public class ProfesseurColorRegistry {

    // ── Palette pastel harmonieuse — assez claires pour le texte noir ────────
    private static final Color[] PALETTE = {
        new Color(0xFFE0B2), // orange pastel
        new Color(0xC8E6C9), // vert pastel
        new Color(0xBBDEFB), // bleu pastel
        new Color(0xF8BBD9), // rose pastel
        new Color(0xE1BEE7), // violet pastel
        new Color(0xB2EBF2), // cyan pastel
        new Color(0xFFF9C4), // jaune pastel
        new Color(0xFFCCBC), // saumon pastel
        new Color(0xD7CCC8), // brun pastel
        new Color(0xCFD8DC), // gris bleuté pastel
        new Color(0xF0F4C3), // lime pastel
        new Color(0xDCEDC8), // vert clair pastel
        new Color(0xB3E5FC), // bleu ciel pastel
        new Color(0xF3E5F5), // lilas pastel
        new Color(0xFCE4EC), // rouge rosé pastel
        new Color(0xE8F5E9), // menthe pastel
        new Color(0xFFF3E0), // crème pastel
        new Color(0xEDE7F6), // lavande pastel
        new Color(0xE0F2F1), // turquoise pastel
        new Color(0xFBE9E7), // corail pastel
    };

    // Cache : id professeur → couleur assignée
    private final Map<String, Color> cache = new LinkedHashMap<>();

    // Singleton léger (non statique pour pouvoir être réinitialisé avec AppContext)
    private static ProfesseurColorRegistry instance;

    private ProfesseurColorRegistry() {}

    public static ProfesseurColorRegistry get() {
        if (instance == null) instance = new ProfesseurColorRegistry();
        return instance;
    }

    /** Remet à zéro le registre (utile lors d'un rechargement de fichier Excel). */
    public static void reset() {
        instance = new ProfesseurColorRegistry();
    }

    // ── API principale ────────────────────────────────────────────────────────

    /**
     * Retourne la couleur AWT associée au professeur.
     * Si le professeur n'est pas encore enregistré, lui assigne
     * la prochaine couleur disponible dans la palette (rotation).
     */
    public Color getCouleur(Professeur prof) {
        if (prof == null) return Color.WHITE;
        return cache.computeIfAbsent(prof.getId(), id -> assignerCouleur(id));
    }

    /**
     * Retourne la couleur sous forme de code hexadécimal HTML (#RRGGBB).
     * Utile pour les logs et les tooltips.
     */
    public String getCouleurHex(Professeur prof) {
        Color c = getCouleur(prof);
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Enregistre en avance tous les professeurs d'une liste.
     * Garantit un ordre déterministe basé sur le hash du nom.
     */
    public void enregistrerTous(List<Professeur> professeurs) {
        // Trier par id pour un ordre stable indépendant de l'ordre de chargement
        professeurs.stream()
            .sorted(Comparator.comparing(Professeur::getId))
            .forEach(this::getCouleur);
    }

    /**
     * Retourne tous les couples (professeur id → couleur) enregistrés.
     */
    public Map<String, Color> getToutesCouleurs() {
        return Collections.unmodifiableMap(cache);
    }

    // ── Algorithme d'assignation ──────────────────────────────────────────────

    /**
     * Assigne une couleur de la palette via hash du nom du prof.
     * Reproductible : même nom → même couleur à chaque exécution.
     */
    private Color assignerCouleur(String profId) {
        int hash = Math.abs(profId.hashCode());
        int idx  = hash % PALETTE.length;

        // Si collision (deux profs ont le même index), décaler
        // Borné à PALETTE.length essais : au-delà de 20 professeurs, les couleurs sont réutilisées
        // (auparavant la boucle ne se terminait jamais quand la palette était épuisée).
        int essais = 0;
        while (cache.values().contains(PALETTE[idx]) && essais++ < PALETTE.length) {
            idx = (idx + 1) % PALETTE.length;
        }
        return PALETTE[idx];
    }

    /**
     * Convertit une Color AWT en Color OpenPDF (com.lowagie.text).
     * Utilisé par PdfPlanningExporter.
     */
    public static java.awt.Color toAwtColor(Color c) {
        return c; // Color est déjà AWT — méthode utilitaire pour la lisibilité
    }

    /**
     * Convertit une Color AWT en octets XSSFColor pour Apache POI.
     * Utilisé par ExcelPlanningExporter.
     */
    public static byte[] toXssfBytes(Color c) {
        return new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()};
    }
}
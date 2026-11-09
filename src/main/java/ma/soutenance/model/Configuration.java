package ma.soutenance.model;

import java.io.Serializable;
import java.time.*;
import java.util.*;

/**
 * Configuration de la session de soutenances.
 *
 * v2.0 — Horaires dynamiques : heureDebutMatin et heureDebutApresMidi
 * sont maintenant lus depuis la feuille Excel "Configuration".
 * Plus aucun horaire défini en dur dans le code.
 */
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate dateDebut;
    private int       nombreJours;
    private int       dureeCreneauMinutes = 60;

    // ── Horaires dynamiques (plus de valeurs en dur) ──────────────────────────
    private LocalTime heureDebutMatin      = LocalTime.of(9,  0);  // défaut 09:00
    private LocalTime heureDebutApresMidi  = LocalTime.of(14, 0);  // défaut 14:00
    private LocalTime heureFinMatin        = LocalTime.of(12, 0);  // défaut 12:00
    private LocalTime heureFinApresMidi    = LocalTime.of(18, 0);  // défaut 18:00

    // ── Constructeurs ─────────────────────────────────────────────────────────
    public Configuration() {}

    /** Constructeur minimal — compatibilité ascendante avec l'ancien code. */
    public Configuration(LocalDate dateDebut, int nombreJours) {
        this.dateDebut    = dateDebut;
        this.nombreJours  = nombreJours;
    }

    /** Constructeur complet avec horaires dynamiques. */
    public Configuration(LocalDate dateDebut, int nombreJours,
                         LocalTime heureDebutMatin, LocalTime heureDebutApresMidi,
                         LocalTime heureFinMatin,   LocalTime heureFinApresMidi) {
        this.dateDebut           = dateDebut;
        this.nombreJours         = nombreJours;
        this.heureDebutMatin     = heureDebutMatin;
        this.heureDebutApresMidi = heureDebutApresMidi;
        this.heureFinMatin       = heureFinMatin;
        this.heureFinApresMidi   = heureFinApresMidi;
    }

    // ── Génération des créneaux horaires ──────────────────────────────────────

    /**
     * Génère la liste des heures de début de créneaux pour une journée.
     * Construit deux plages : matin (heureDebutMatin → heureFinMatin)
     * et après-midi (heureDebutApresMidi → heureFinApresMidi).
     * La durée d'un créneau est dureeCreneauMinutes.
     */
    public List<LocalTime> getHeuresDebut() {
        List<LocalTime> heures = new ArrayList<>();

        // Plage matin
        LocalTime h = heureDebutMatin;
        while (!h.plusMinutes(dureeCreneauMinutes).isAfter(heureFinMatin)) {
            heures.add(h);
            h = h.plusMinutes(dureeCreneauMinutes);
        }

        // Plage après-midi
        h = heureDebutApresMidi;
        while (!h.plusMinutes(dureeCreneauMinutes).isAfter(heureFinApresMidi)) {
            heures.add(h);
            h = h.plusMinutes(dureeCreneauMinutes);
        }

        return heures;
    }

    /** Capacité totale = nbSalles × nbCréneauxParJour × nbJours. */
    public int getCapaciteTotale(int nbSalles) {
        return nbSalles * getHeuresDebut().size() * nombreJours;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public LocalDate getDateDebut()              { return dateDebut; }
    public void      setDateDebut(LocalDate d)   { this.dateDebut = d; }

    public int  getNombreJours()                 { return nombreJours; }
    public void setNombreJours(int n)            { this.nombreJours = n; }

    public int  getDureeCreneauMinutes()         { return dureeCreneauMinutes; }
    public void setDureeCreneauMinutes(int d)    { this.dureeCreneauMinutes = d; }

    public LocalTime getHeureDebutMatin()        { return heureDebutMatin; }
    public void      setHeureDebutMatin(LocalTime h) { this.heureDebutMatin = h; }

    public LocalTime getHeureDebutApresMidi()    { return heureDebutApresMidi; }
    public void      setHeureDebutApresMidi(LocalTime h) { this.heureDebutApresMidi = h; }

    public LocalTime getHeureFinMatin()          { return heureFinMatin; }
    public void      setHeureFinMatin(LocalTime h)    { this.heureFinMatin = h; }

    public LocalTime getHeureFinApresMidi()      { return heureFinApresMidi; }
    public void      setHeureFinApresMidi(LocalTime h) { this.heureFinApresMidi = h; }

    @Override
    public String toString() {
        return "Configuration{date=" + dateDebut + ", jours=" + nombreJours
            + ", matin=" + heureDebutMatin + "-" + heureFinMatin
            + ", apm=" + heureDebutApresMidi + "-" + heureFinApresMidi + "}";
    }
}
package ma.soutenance.service.impl;

import ma.soutenance.exception.CapaciteDepasseeException;
import java.time.Duration;
import ma.soutenance.model.*;
import ma.soutenance.model.enums.*;
import ma.soutenance.service.PlanningService;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'affectation — Load Balancing strict (⌊M⌋/⌈M⌉) + PriorityQueue.
 * v2.0 — filiereToSpec() utilise FiliereDynamique.getSpecialiteEncadrement()
 */
public class AffectationService implements PlanningService {

    private final List<Professeur> professeurs;
    private final List<Salle>      salles;
    private final Configuration    config;
    private final Random           rng = new Random();

    public AffectationService(List<Professeur> professeurs,
                               List<Salle>      salles,
                               Configuration    config) {
        this.professeurs = new ArrayList<>(professeurs);
        this.salles      = new ArrayList<>(salles);
        this.config      = config;
    }

    /** Nombre maximal de tirages aléatoires avant d'abandonner. */
    private static final int MAX_TENTATIVES = 1000;

    @Override
    public Planning genererPlanning(List<Groupe> groupes) {
        int capacite = config.getCapaciteTotale(salles.size());
        if (groupes.size() > capacite)
            throw new CapaciteDepasseeException(groupes.size(), capacite);

        // Le placement est glouton sur un tirage aléatoire (encadrants, jurys, ordre) : un tirage
        // peut se bloquer sur les dernières soutenances alors qu'une solution existe. On retente
        // donc avec un nouveau tirage ; les règles (jury, repos, salles) restent strictement les mêmes.
        IllegalStateException derniereErreur = null;
        for (int tentative = 1; tentative <= MAX_TENTATIVES; tentative++) {
            try {
                return tenterGeneration(groupes);
            } catch (IllegalStateException e) {
                derniereErreur = e;
            }
        }
        throw derniereErreur;
    }

    /** Une tentative de génération (un tirage aléatoire). */
    private Planning tenterGeneration(List<Groupe> groupes) {
        Map<Professeur, List<Groupe>> affectation = affecterEncadrants(groupes, professeurs);
        List<Creneau> grille   = buildGrille();
        Planning      planning = new Planning(config);

        Map<Professeur, Set<Creneau>> occupations    = new HashMap<>();
        Set<String>                   sallesOccupees = new HashSet<>();
        List<Soutenance>              soutenances    = new ArrayList<>();

        for (Map.Entry<Professeur, List<Groupe>> entry : affectation.entrySet()) {
            Professeur enc = entry.getKey();
            for (Groupe g : entry.getValue()) {
                List<Professeur> jury = constituerJury(g, enc);
                // Un groupe en anglais exige un prof d'anglais au jury : si le tirage a désigné ce prof
                // comme encadrant, il ne peut pas siéger -> on rejette le tirage (nouvelle tentative).
                if (g.isAnglais() && jury.stream().noneMatch(p -> p.getSpecialite() == Specialite.ANGLAIS)
                        && professeurs.stream().anyMatch(p -> p.getSpecialite() == Specialite.ANGLAIS))
                    throw new IllegalStateException(
                        "Impossible de constituer un jury conforme pour un groupe en anglais.");
                soutenances.add(new Soutenance(g, enc, jury, null));
            }
        }

        Collections.shuffle(soutenances, rng);

        for (Soutenance soutenance : soutenances) {
            Creneau creneau = trouverCreneauLibre(
                grille, soutenance.getJuryComplet(), occupations, sallesOccupees);

            if (creneau == null)
                throw new IllegalStateException(
                    "Impossible de trouver un créneau libre. Augmentez les jours ou les salles.");

            soutenance.setCreneau(creneau);

            String cleSalle = creneau.getSalle().getNom() + "_"
                + creneau.getDate() + "_" + creneau.getHeureDebut();
            sallesOccupees.add(cleSalle);

            for (Professeur p : soutenance.getJuryComplet())
                occupations.computeIfAbsent(p, k -> new HashSet<>()).add(creneau);

            planning.addSoutenance(soutenance);
        }

        return planning;
    }

    private Creneau trouverCreneauLibre(List<Creneau> grille,
                                         List<Professeur> jury,
                                         Map<Professeur, Set<Creneau>> occupations,
                                         Set<String> sallesOccupees) {
        for (Creneau candidat : grille) {
            String cleSalle = candidat.getSalle().getNom() + "_"
                + candidat.getDate() + "_" + candidat.getHeureDebut();
            if (sallesOccupees.contains(cleSalle)) continue;

            boolean juryLibre = jury.stream().allMatch(prof -> {
                Set<Creneau> cp = occupations.getOrDefault(prof, Set.of());
                return cp.stream().allMatch(c -> {
                    if (!c.getDate().equals(candidat.getDate())) return true;
                    if (c.overlaps(candidat)) return false;
                    long gapApres = Duration.between(c.getHeureFin(), candidat.getHeureDebut()).toMinutes();
                    long gapAvant = Duration.between(candidat.getHeureFin(), c.getHeureDebut()).toMinutes();
                    boolean pdApres = !c.getHeureFin().isAfter(config.getHeureFinMatin()) && !candidat.getHeureDebut().isBefore(config.getHeureDebutApresMidi());
                    boolean pdAvant = !candidat.getHeureFin().isAfter(config.getHeureFinMatin()) && !c.getHeureDebut().isBefore(config.getHeureDebutApresMidi());
                    if (gapApres >= 0 && gapApres < 60 && !pdApres) return false;
                    if (gapAvant >= 0 && gapAvant < 60 && !pdAvant) return false;
                    return true;
                });
            });

            if (juryLibre) return candidat;
        }
        return null;
    }

    public Map<Professeur, List<Groupe>> affecterEncadrants(
            List<Groupe> groupes, List<Professeur> profs) {

        validerEntrees(groupes, profs);

        List<Groupe>     gMel = new ArrayList<>(groupes);
        List<Professeur> pMel = new ArrayList<>(profs);
        Collections.shuffle(gMel, rng);
        Collections.shuffle(pMel, rng);

        pMel.forEach(Professeur::resetCharge);

        int n = gMel.size(), p = pMel.size();
        int plancher  = n / p;
        int plafond   = (n + p - 1) / p;
        int nbPlafond = n % p;

        Map<Professeur, Integer> quotaMax = new LinkedHashMap<>();
        for (int i = 0; i < pMel.size(); i++)
            quotaMax.put(pMel.get(i), i < nbPlafond ? plafond : plancher);

        Map<Professeur, List<Groupe>> affectation = new LinkedHashMap<>();
        pMel.forEach(prof -> affectation.put(prof, new ArrayList<>()));

        PriorityQueue<Professeur> pq = new PriorityQueue<>(
            Comparator.comparingInt(Professeur::getChargeActuelle)
                      .thenComparingInt(prof -> -(quotaMax.get(prof) - prof.getChargeActuelle())));
        pq.addAll(pMel);

        for (Groupe groupe : gMel) {
            Specialite cible = filiereToSpec(groupe.getFiliere());
            Professeur encadrant = extraireParSpecialite(pq, cible, quotaMax);
            if (encadrant == null) encadrant = pq.poll();
            if (encadrant == null)
                throw new IllegalStateException("Plus aucun prof disponible.");

            encadrant.incrementCharge();
            affectation.get(encadrant).add(groupe);

            if (encadrant.getChargeActuelle() < quotaMax.get(encadrant))
                pq.add(encadrant);
        }

        return affectation;
    }

    private Professeur extraireParSpecialite(PriorityQueue<Professeur> pq,
                                              Specialite cible,
                                              Map<Professeur, Integer> quotaMax) {
        List<Professeur> misDecote = new ArrayList<>();
        Professeur trouve = null;
        while (!pq.isEmpty()) {
            Professeur candidat = pq.poll();
            if (candidat.getSpecialite() == cible) { trouve = candidat; break; }
            misDecote.add(candidat);
        }
        pq.addAll(misDecote);
        return trouve;
    }

    private List<Professeur> constituerJury(Groupe g, Professeur enc) {
        List<Professeur> dispo = professeurs.stream()
            .filter(p -> !p.equals(enc))
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(dispo, rng);

        List<Professeur> jury = new ArrayList<>();
        if (g.isAnglais()) {
            dispo.stream().filter(p -> p.getSpecialite() == Specialite.ANGLAIS)
                .findFirst().ifPresent(p -> { jury.add(p); dispo.remove(p); });
            dispo.stream().filter(p -> p.getSpecialite() != Specialite.ANGLAIS)
                .findFirst().ifPresent(p -> { jury.add(p); dispo.remove(p); });
            while (jury.size() < 2 && !dispo.isEmpty()) jury.add(dispo.remove(0));
        } else {
            for (int i = 0; i < 2 && i < dispo.size(); i++) jury.add(dispo.get(i));
        }
        return jury;
    }

    private List<Creneau> buildGrille() {
        List<Creneau>   grille = new ArrayList<>();
        List<LocalTime> heures = config.getHeuresDebut();
        LocalDate       date   = config.getDateDebut();
        int             dur    = config.getDureeCreneauMinutes();

        for (int j = 0; j < config.getNombreJours(); j++) {
            while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                date = date.plusDays(1);
            for (LocalTime h : heures)
                for (Salle s : salles)
                    grille.add(new Creneau(date, h, h.plusMinutes(dur), s));
            date = date.plusDays(1);
        }
        return grille;
    }

    /**
     * v2.0 — utilise Filiere.getSpecialiteEncadrement()
     * au lieu du switch sur l'enum Filiere.
     */
    private Specialite filiereToSpec(Filiere f) {
        if (f == null) return Specialite.INFORMATIQUE;
        return Specialite.fromString(f.getSpecialiteEncadrement());
    }

    private void validerEntrees(List<Groupe> groupes, List<Professeur> profs) {
        Objects.requireNonNull(groupes, "La liste des groupes ne peut pas être null.");
        Objects.requireNonNull(profs,   "La liste des professeurs ne peut pas être null.");
        if (profs.isEmpty())  throw new IllegalArgumentException("Aucun professeur disponible.");
        if (groupes.isEmpty()) throw new IllegalArgumentException("Aucun groupe à affecter.");
    }

    public String rapportCharge(Map<Professeur, List<Groupe>> affectation) {
        StringBuilder sb = new StringBuilder("=== Rapport Affectation ===\n");
        affectation.forEach((prof, liste) ->
            sb.append(String.format("  %-30s [%-15s] → %d groupe(s)%n",
                prof.getFullName(), prof.getSpecialite().getLibelle(), liste.size())));
        int total = affectation.values().stream().mapToInt(List::size).sum();
        int min   = affectation.values().stream().mapToInt(List::size).min().orElse(0);
        int max   = affectation.values().stream().mapToInt(List::size).max().orElse(0);
        sb.append(String.format("  Total : %d | Min : %d | Max : %d | Écart : %d%n",
            total, min, max, max - min));
        return sb.toString();
    }
}
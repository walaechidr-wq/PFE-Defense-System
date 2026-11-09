package ma.soutenance.service.impl;

import ma.soutenance.model.*;
import ma.soutenance.model.enums.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;

/**
 * Tests unitaires pour {@link AffectationService#affecterEncadrants}
 * — algorithme Round-Robin.
 *
 * Couvre :
 * - Équité cyclique (écart max = 1 entre tous les profs)
 * - Couverture totale (tous les groupes affectés exactement une fois)
 * - Aucun prof sans groupe (quand nbGroupes >= nbProfs)
 * - Respect des spécialités (priorisation par filière)
 * - Fallback quand spécialité manquante
 * - Cas limites (1 prof, N=N, listes vides, null)
 * - Caractère dynamique (shuffle ne modifie pas la liste originale)
 */
class AffectationServiceTest {
	// Constantes de test — remplacent Filiere.GI, Filiere.TDIA
	private static final Filiere F_GI   =
	    new Filiere("GI",   "Génie Informatique",            "INFORMATIQUE");
	private static final Filiere F_TDIA =
	    new Filiere("TDIA", "Transformation Digitale et IA", "MATHEMATIQUE");
	private static final Filiere F_ID   =
	    new Filiere("ID",   "Ingénierie des Données",         "INFORMATIQUE");

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS DE FABRICATION
    // ════════════════════════════════════════════════════════════════════════

    private static Professeur prof(String id, Specialite spec) {
        return new Professeur(id, "Prof", id, spec);
    }
 // APRÈS
    private static Groupe groupe(Filiere filiere) {
        return new Groupe(Langue.FRANCAIS, filiere);
    }

    /**
     * Instancie le service Round-Robin.
     * Les salles et config sont null car on ne teste pas genererPlanning.
     */
    private AffectationService service(List<Professeur> profs) {
        return new AffectationService(profs, List.of(), null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  1. ÉQUITÉ CYCLIQUE — écart max = 1
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Division exacte : chaque prof reçoit exactement N/P groupes")
    void equite_divisionExacte() {
        // 3 profs, 9 groupes → 9/3 = 3 exactement chacun
        List<Professeur> profs   = nProfs(3, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(9, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        int min = result.values().stream().mapToInt(List::size).min().orElse(0);
        int max = result.values().stream().mapToInt(List::size).max().orElse(0);

        assertEquals(0, max - min,
            "Division exacte : tous les profs doivent avoir le même nombre de groupes");
        result.values().forEach(l ->
            assertEquals(3, l.size(), "Charge attendue = 3"));
    }

    @Test
    @DisplayName("Division non exacte : écart de charge max = 1 (Round-Robin)")
    void equite_divisionNonExacte() {
        // 3 profs, 10 groupes → certains auront 3, d'autres 4, écart = 1
        List<Professeur> profs   = nProfs(3, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(10, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        int min = result.values().stream().mapToInt(List::size).min().orElse(0);
        int max = result.values().stream().mapToInt(List::size).max().orElse(0);

        assertTrue(max - min <= 1,
            "Round-Robin : écart max autorisé = 1, obtenu : min=" + min + " max=" + max);
    }

    @ParameterizedTest(name = "{0} profs, {1} groupes")
    @CsvSource({"2,5", "4,7", "5,13", "3,11", "7,20", "1,6", "6,6"})
    @DisplayName("Équité Round-Robin : écart <= 1 pour différentes tailles")
    void equite_diversesTailles(int nbProfs, int nbGroupes) {
        List<Professeur> profs   = nProfs(nbProfs, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(nbGroupes,F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        int min   = result.values().stream().mapToInt(List::size).min().orElse(0);
        int max   = result.values().stream().mapToInt(List::size).max().orElse(0);
        int total = result.values().stream().mapToInt(List::size).sum();

        assertAll(
            () -> assertTrue(max - min <= 1,
                "Ecart de charge > 1 : min=" + min + " max=" + max),
            () -> assertEquals(nbGroupes, total,
                "Le total des groupes affectes doit etre " + nbGroupes)
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  2. COUVERTURE TOTALE
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Tous les groupes sont affectés exactement une fois")
    void tousLesGroupesSontAffectes() {
        List<Professeur> profs   = nProfs(3, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(9, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        Set<Groupe> affectes = result.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        assertEquals(new HashSet<>(groupes), affectes,
            "Des groupes sont manquants ou dupliques dans l'affectation");
    }

    @Test
    @DisplayName("Somme totale = nombre de groupes (intégrité de l'affectation)")
    void sommeTotaleCorrecte() {
        List<Professeur> profs   = nProfs(4, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(11, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        int total = result.values().stream().mapToInt(List::size).sum();
        assertEquals(11, total, "11 groupes doivent etre affectes au total");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  3. AUCUN PROF SANS GROUPE
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Chaque prof reçoit au moins 1 groupe quand nbGroupes >= nbProfs")
    void aucunProfSansGroupe() {
        List<Professeur> profs   = nProfs(4, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(8, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        result.values().forEach(liste ->
            assertFalse(liste.isEmpty(), "Un prof n'a recu aucun groupe !"));
    }

    @Test
    @DisplayName("Autant de profs que de groupes → chacun reçoit exactement 1")
    void casLimite_autantDeProfQueDeGroupes() {
        List<Professeur> profs   = nProfs(5, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(5, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        result.values().forEach(l ->
            assertEquals(1, l.size(), "Chaque prof doit recevoir exactement 1 groupe"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  4. RESPECT DES SPÉCIALITÉS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Filière GI → priorisation INFORMATIQUE")
    void specialite_GI_informatique() {
        Professeur info = prof("INFO", Specialite.INFORMATIQUE);
        Professeur math = prof("MATH", Specialite.MATHEMATIQUE);
        List<Professeur> profs   = List.of(info, math);
        List<Groupe>     groupes = nGroupes(4, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        assertTrue(result.get(info).size() >= result.get(math).size(),
            "Le prof INFO doit etre prioritaire pour les groupes GI");
    }

    @Test
    @DisplayName("Filière TDIA → priorisation MATHEMATIQUE")
    void specialite_TDIA_mathematique() {
        Professeur math = prof("MATH", Specialite.MATHEMATIQUE);
        Professeur info = prof("INFO", Specialite.INFORMATIQUE);
        List<Professeur> profs   = List.of(math, info);
        List<Groupe>     groupes = nGroupes(4,F_TDIA);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        assertTrue(result.get(math).size() >= result.get(info).size(),
            "Le prof MATH doit etre prioritaire pour les groupes TDIA");
    }

    @Test
    @DisplayName("Groupes mixtes GI+TDIA : équité + spécialité respectées")
    void specialite_groupesMixtes() {
        Professeur info = prof("INFO", Specialite.INFORMATIQUE);
        Professeur math = prof("MATH", Specialite.MATHEMATIQUE);
        List<Professeur> profs = List.of(info, math);

        List<Groupe> groupes = new ArrayList<>();
        groupes.add(groupe(F_GI));
        groupes.add(groupe(F_TDIA));
        groupes.add(groupe(F_GI));
        groupes.add(groupe(F_TDIA));

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        int total = result.values().stream().mapToInt(List::size).sum();
        assertEquals(4, total, "Tous les groupes doivent etre affectes");
        assertEquals(2, result.get(info).size());
        assertEquals(2, result.get(math).size());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  5. FALLBACK
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Fallback : aucun prof MATH → groupes TDIA affectés quand même")
    void fallback_aucunProfMath() {
        List<Professeur> profs = List.of(
            prof("P1", Specialite.INFORMATIQUE),
            prof("P2", Specialite.INFORMATIQUE)
        );
        List<Groupe> groupes = nGroupes(4, F_TDIA);

        assertDoesNotThrow(() -> {
            Map<Professeur, List<Groupe>> result =
                service(profs).affecterEncadrants(groupes, profs);
            int total = result.values().stream().mapToInt(List::size).sum();
            assertEquals(4, total,
                "Tous les groupes doivent etre affectes via fallback");
        });
    }

    @Test
    @DisplayName("Fallback total : spécialité GESTION pour des groupes GI")
    void fallback_specialiteGestion() {
        List<Professeur> profs = List.of(
            prof("G1", Specialite.GESTION),
            prof("G2", Specialite.GESTION)
        );
        List<Groupe> groupes = nGroupes(4, F_GI);

        assertDoesNotThrow(() -> {
            Map<Professeur, List<Groupe>> result =
                service(profs).affecterEncadrants(groupes, profs);
            int total = result.values().stream().mapToInt(List::size).sum();
            assertEquals(4, total);
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  6. CAS LIMITES
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("1 seul prof → reçoit tous les groupes")
    void casLimite_unSeulProf() {
        List<Professeur> profs   = List.of(prof("P1", Specialite.INFORMATIQUE));
        List<Groupe>     groupes = nGroupes(7, F_GI);

        Map<Professeur, List<Groupe>> result =
            service(profs).affecterEncadrants(groupes, profs);

        assertEquals(7, result.get(profs.get(0)).size(),
            "Le seul prof disponible doit recevoir tous les groupes");
    }

    @Test
    @DisplayName("Exception si liste de profs vide")
    void exception_listeProfVide() {
        assertThrows(IllegalArgumentException.class,
            () -> service(List.of())
                .affecterEncadrants(nGroupes(3, F_GI), List.of()));
    }

    @Test
    @DisplayName("Exception si liste de groupes vide")
    void exception_listeGroupeVide() {
        List<Professeur> profs = nProfs(2, Specialite.INFORMATIQUE);
        assertThrows(IllegalArgumentException.class,
            () -> service(profs).affecterEncadrants(List.of(), profs));
    }

    @Test
    @DisplayName("Exception si null passé en paramètre")
    void exception_parametresNull() {
        List<Professeur> profs = nProfs(2, Specialite.INFORMATIQUE);
        assertAll(
            () -> assertThrows(NullPointerException.class,
                () -> service(profs).affecterEncadrants(null, profs)),
            () -> assertThrows(NullPointerException.class,
                () -> service(profs).affecterEncadrants(nGroupes(2, F_GI), null))
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  7. CARACTÈRE DYNAMIQUE
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deux runs successifs produisent des résultats valides")
    void dynamique_deuxRunsValides() {
        List<Professeur> profs   = nProfs(3, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(9, F_GI);

        Map<Professeur, List<Groupe>> run1 =
            service(profs).affecterEncadrants(groupes, new ArrayList<>(profs));
        Map<Professeur, List<Groupe>> run2 =
            service(profs).affecterEncadrants(groupes, new ArrayList<>(profs));

        int total1 = run1.values().stream().mapToInt(List::size).sum();
        int total2 = run2.values().stream().mapToInt(List::size).sum();

        assertEquals(9, total1, "Run 1 : total incorrect");
        assertEquals(9, total2, "Run 2 : total incorrect");
    }

    @Test
    @DisplayName("La liste originale des groupes n'est pas modifiée par le service")
    void dynamique_listeOriginaleIntacte() {
        List<Professeur> profs   = nProfs(3, Specialite.INFORMATIQUE);
        List<Groupe>     groupes = nGroupes(9, F_GI);
        List<Groupe>     copie   = new ArrayList<>(groupes);

        service(profs).affecterEncadrants(groupes, profs);

        assertEquals(copie, groupes,
            "Le service ne doit pas modifier la liste originale des groupes");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private List<Professeur> nProfs(int n, Specialite spec) {
        List<Professeur> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(prof("P" + i, spec));
        return list;
    }

 // APRÈS
    private List<Groupe> nGroupes(int n, Filiere filiere) {
        List<Groupe> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(groupe(filiere));
        return list;
    }
}
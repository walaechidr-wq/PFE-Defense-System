package ma.soutenance.model;

import java.util.*;

/**
 * Registre centralisé des filières dynamiques.
 * Remplace Filiere.fromString() partout dans le projet.
 * Initialisé par AppContext au démarrage depuis la feuille Excel "Filieres".
 */
public class FiliereRegistry {

    private static final Map<String, Filiere> MAP = new LinkedHashMap<>();

    private static final Filiere DEFAUT =
        new Filiere("GI", "Génie Informatique", "INFORMATIQUE");

    /** Appelé par AppContext.charger() — initialise le registre depuis Excel. */
    public static void init(List<Filiere> filieres) {
        MAP.clear();
        filieres.forEach(f -> MAP.put(f.getCode().toUpperCase(), f));
    }

    /** Équivalent de Filiere.fromString() */
    public static Filiere fromString(String s) {
        if (s == null || s.isBlank()) return DEFAUT;
        return MAP.getOrDefault(s.trim().toUpperCase(), DEFAUT);
    }

    /** Toutes les filières disponibles. */
    public static Collection<Filiere> getAll() {
        return Collections.unmodifiableCollection(MAP.values());
    }

    public static boolean isEmpty() { return MAP.isEmpty(); }
}
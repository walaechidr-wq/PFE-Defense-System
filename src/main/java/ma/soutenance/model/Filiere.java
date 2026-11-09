package ma.soutenance.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Filière chargée dynamiquement depuis la feuille Excel "Filieres".
 * Remplace l'enum Filiere — principe OCP.
 */
public class Filiere implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String libelle;
    private final String specialiteEncadrement;

    public Filiere(String code, String libelle, String specialiteEncadrement) {
        this.code                  = code.trim().toUpperCase();
        this.libelle               = libelle.trim();
        this.specialiteEncadrement = (specialiteEncadrement != null)
            ? specialiteEncadrement.trim().toUpperCase() : "INFORMATIQUE";
    }

    public String getCode()                  { return code; }
    public String getLibelle()               { return libelle; }
    public String getSpecialiteEncadrement() { return specialiteEncadrement; }

    /** Compatibilité avec les appels à .name() de l'ancien enum. */
    public String name() { return code; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filiere)) return false;
        return Objects.equals(code, ((Filiere) o).code);
    }

    @Override public int hashCode()   { return Objects.hash(code); }
    @Override public String toString() { return code + " (" + libelle + ")"; }
}

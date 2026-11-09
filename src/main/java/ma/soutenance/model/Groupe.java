package ma.soutenance.model;

import ma.soutenance.model.enums.Langue;
import java.io.Serializable;
import java.util.*;

/**
 * Entité Groupe — v2.0 : utilise FiliereDynamique au lieu de l'enum Filiere.
 */
public class Groupe implements Serializable {

    private static final long serialVersionUID = 1L;

    private String           id;
    private List<Etudiant>   etudiants = new ArrayList<>();
    private Langue           langue;
    private Filiere filiere;

    public Groupe() {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Groupe(Langue langue, Filiere filiere) {
        this();
        this.langue  = langue;
        this.filiere = filiere;
    }

    public void addEtudiant(Etudiant e) { if (e != null) etudiants.add(e); }

    public boolean isBinome()  { return etudiants.size() == 2; }
    public boolean isAnglais() { return Langue.ANGLAIS == langue; }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etudiants.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(etudiants.get(i).getFullName());
        }
        return sb.toString();
    }

    public String           getId()       { return id; }
    public void             setId(String i){ id = i; }

    public List<Etudiant>   getEtudiants()              { return etudiants; }
    public void             setEtudiants(List<Etudiant> e){ etudiants = e; }

    public Langue           getLangue()          { return langue; }
    public void             setLangue(Langue l)  { langue = l; }

    public Filiere getFiliere()             { return filiere; }
    public void             setFiliere(Filiere f){ filiere = f; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Groupe)) return false;
        return Objects.equals(id, ((Groupe) o).id);
    }

    @Override public int hashCode()    { return Objects.hash(id); }
    @Override public String toString() { return "[" + filiere + "] " + getLabel(); }
}
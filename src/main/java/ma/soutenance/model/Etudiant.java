package ma.soutenance.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entité Etudiant — v2.0 : utilise FiliereDynamique au lieu de l'enum Filiere.
 */
public class Etudiant implements Serializable {

    private static final long serialVersionUID = 1L;

    private String          cne;
    private String          nom;
    private String          prenom;
    private String          emailPersonnel;
    private String          emailAcademique;
    private Filiere filiere;

    public Etudiant() {}

    public Etudiant(String cne, String nom, String prenom,
                    String ep, String ea, Filiere f) {
        this.cne            = cne;
        this.nom            = nom;
        this.prenom         = prenom;
        this.emailPersonnel = ep;
        this.emailAcademique= ea;
        this.filiere        = f;
    }

    public String getFullName()    { return nom + " " + prenom; }

    public String getCne()         { return cne; }
    public void   setCne(String c) { cne = c; }

    public String getNom()         { return nom; }
    public void   setNom(String n) { nom = n; }

    public String getPrenom()          { return prenom; }
    public void   setPrenom(String p)  { prenom = p; }

    public String getEmailPersonnel()          { return emailPersonnel; }
    public void   setEmailPersonnel(String e)  { emailPersonnel = e; }

    public String getEmailAcademique()         { return emailAcademique; }
    public void   setEmailAcademique(String e) { emailAcademique = e; }

    public Filiere getFiliere()          { return filiere; }
    public void             setFiliere(Filiere f) { filiere = f; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etudiant)) return false;
        return Objects.equals(cne, ((Etudiant) o).cne);
    }

    @Override public int hashCode()    { return Objects.hash(cne); }
    @Override public String toString() { return getFullName() + " [" + filiere + "]"; }
}
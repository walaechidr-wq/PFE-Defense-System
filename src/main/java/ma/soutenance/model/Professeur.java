package ma.soutenance.model;
import ma.soutenance.model.enums.Specialite;
import java.io.Serializable;
import java.util.Objects;
public class Professeur implements Serializable {
	private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String prenom;
    private Specialite specialite;
    private int chargeActuelle;
    
    public Professeur(){}
    public Professeur(String id,String nom,String prenom,Specialite s){
        this.id=id;
        this.nom=nom;
        this.prenom=prenom;
        this.specialite=s;
        this.chargeActuelle=0;
    }
    public String getFullName(){
    	return nom+" "+prenom;
    	}
    public void incrementCharge(){
    	chargeActuelle++;
    	}
    public void resetCharge(){
    	chargeActuelle=0;
    	}
    public String getId(){
    	return id;
    	} 
    public void setId(String i){
    	id=i;
    	}
    public String getNom(){
    	return nom;
    	} 
    public void setNom(String n){
    	nom=n;
    	}
    public String getPrenom(){
    	return prenom;
    	} 
    public void setPrenom(String p){
    	prenom=p;
    	}
    public Specialite getSpecialite(){
    	return specialite;
    	} 
    public void setSpecialite(Specialite s){
    	specialite=s;
    	}
    public int getChargeActuelle(){
    	return chargeActuelle;
    	} 
    public void setChargeActuelle(int c){
    	chargeActuelle=c;
    	}
    @Override public boolean equals(Object o){
    	if(this==o)return true;
    	if(!(o instanceof Professeur))return false;
    	return Objects.equals(id,((Professeur)o).id);
    	}
    @Override public int hashCode(){
    	return Objects.hash(id);
    	}
    @Override public String toString(){
    	return nom+" "+prenom+" ("+specialite+")";
    	}
}

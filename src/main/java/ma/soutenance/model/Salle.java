package ma.soutenance.model;
import java.io.Serializable;
import java.util.Objects;
public class Salle implements Serializable {
	private static final long serialVersionUID = 1L;

    private String nom;
    public Salle(){}
    public Salle(String nom){
    	this.nom=nom;
    	}
    public String getNom(){
    	return nom;
    	} 
    public void setNom(String n){
    	nom=n;
    	}
    @Override public boolean equals(Object o){
    	if(this==o)return true;
    	if(!(o instanceof Salle))return false;
    	return Objects.equals(nom,((Salle)o).nom);
    	}
    @Override public int hashCode(){
    	return Objects.hash(nom);
    	}
    @Override public String toString(){
    	return nom;
    	}
}

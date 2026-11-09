package ma.soutenance.model;
import java.io.Serializable;
import java.util.*;
public class Soutenance implements Serializable {
	private static final long serialVersionUID = 1L;

    private String id;
    private Groupe groupe;
    private Professeur encadrant;
    private List<Professeur> autresJury=new ArrayList<>();
    private Creneau creneau;
    public Soutenance(){
    	this.id=UUID.randomUUID().toString().substring(0,12).toUpperCase();
    	}
    public Soutenance(Groupe g,Professeur enc,List<Professeur> jury,Creneau c){
    	this();
    	groupe=g;
    	encadrant=enc;
    	autresJury=jury;
    	creneau=c;
    	}
    public List<Professeur> getJuryComplet(){
    	List<Professeur> l=new ArrayList<>();
    	l.add(encadrant);
    	l.addAll(autresJury);
    	return l;
    	}
    public boolean isAnglais(){
    	return groupe!=null && groupe.isAnglais();
    	}
    public String getSalleNom(){
    	return creneau!=null && creneau.getSalle()!=null?creneau.getSalle().getNom():"N/A";
    	}
    public String getId(){
    	return id;
    	}
    public void setId(String i){
    	id=i;
    	}
    public Groupe getGroupe(){
    	return groupe;
    	} 
    public void setGroupe(Groupe g){
    	groupe=g;
    	}
    public Professeur getEncadrant(){
    	return encadrant;
    	} 
    public void setEncadrant(Professeur p){
    	this.encadrant=p;
    	}
    public List<Professeur> getAutresJury(){
    	return autresJury;
    	} 
    public void setAutresJury(List<Professeur> j){
    	autresJury=j;
    	}
    public Creneau getCreneau(){
    	return creneau;
    	}
    public void setCreneau(Creneau c){
    	this.creneau=c;
    	}
    @Override 
    public boolean equals(Object o){
    	if(this==o)return true;
    	if(!(o instanceof Soutenance))return false;
    	return Objects.equals(id,((Soutenance)o).id);
    	}
    @Override 
    public int hashCode(){
    	return Objects.hash(id);
    	}
}

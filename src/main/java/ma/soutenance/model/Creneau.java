package ma.soutenance.model;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
public class Creneau implements Serializable {
	private static final long serialVersionUID = 1L;

    private LocalDate date;
    private LocalTime heureDebut,heureFin;
    private Salle salle;
    public Creneau(){}
    public Creneau(LocalDate date,LocalTime hd,LocalTime hf,Salle salle){
        this.date=date;
        this.heureDebut=hd;
        this.heureFin=hf;
        this.salle=salle;
    }
    public boolean overlaps(Creneau o){
        if(!this.date.equals(o.date))return false;
        return this.heureDebut.isBefore(o.heureFin)&&o.heureDebut.isBefore(this.heureFin);
    }
    public String getLabel(){
        DateTimeFormatter f=DateTimeFormatter.ofPattern("HH:mm");
        return date+" "+heureDebut.format(f)+"-"+heureFin.format(f);
    }
    public LocalDate getDate(){
    	return date;
    	} 
    public void setDate(LocalDate d){
    	date=d;
    	}
    public LocalTime getHeureDebut(){
    	return heureDebut;
    	} 
    public void setHeureDebut(LocalTime h){
    	heureDebut=h;
    	}
    public LocalTime getHeureFin(){
    	return heureFin;
    	} 
    public void setHeureFin(LocalTime h){
    	heureFin=h;
    	}
    public Salle getSalle(){
    	return salle;
    	} 
    public void setSalle(Salle s){
    	salle=s;
    	}
    @Override 
    public boolean equals(Object o){
    	if(this==o)return true;
    	if(!(o instanceof Creneau))return false;
    	Creneau c=(Creneau)o;
    	return Objects.equals(date,c.date)&&Objects.equals(heureDebut,c.heureDebut)&&Objects.equals(salle,c.salle);}
    @Override 
    public int hashCode(){
    	return Objects.hash(date,heureDebut,salle);
    	}
    @Override 
    public String toString(){
    	return getLabel()+(salle!=null?salle.getNom():"?");
    	}
}

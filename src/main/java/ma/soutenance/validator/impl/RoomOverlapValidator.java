package ma.soutenance.validator.impl;
import ma.soutenance.model.*;
import ma.soutenance.validator.Validator;
import java.util.*;
public class RoomOverlapValidator implements Validator {
    private final List<String> errors=new ArrayList<>();
    @Override public boolean validate(Planning p){
        errors.clear();
        List<Soutenance> s=p.getSoutenances();
        for(int i=0;i<s.size();i++) 
        	for(int j=i+1;j<s.size();j++){
        		Soutenance a=s.get(i),b=s.get(j);
        		if(a.getCreneau()==null||b.getCreneau()==null) continue;
        		if(a.getCreneau().getSalle()==null||b.getCreneau().getSalle()==null) continue;
        		if(a.getSalleNom().equals(b.getSalleNom())&&a.getCreneau().overlaps(b.getCreneau()))
                errors.add("SALLE «"+a.getSalleNom()+"» double réservation le "+a.getCreneau().getDate()+" à "+a.getCreneau().getHeureDebut()+" : "+a.getGroupe().getLabel()+" / "+b.getGroupe().getLabel());
        }
        return errors.isEmpty();
    }
    @Override public List<String> getErrors(){return errors;}
    @Override public String getNom(){return "Chevauchement de salle";}
}

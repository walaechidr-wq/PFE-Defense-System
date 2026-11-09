package ma.soutenance.validator.impl;
import ma.soutenance.model.*;
import ma.soutenance.model.enums.Specialite;
import ma.soutenance.validator.Validator;
import java.util.*;
public class JuryConformiteValidator implements Validator {
    private final List<String> errors=new ArrayList<>();
    @Override public boolean validate(Planning p){
        errors.clear();
        for(Soutenance s:p.getSoutenances()){
            if(s.getEncadrant()==null||s.getGroupe()==null) continue;
            if(s.getAutresJury().size()<2)
                errors.add("JURY INCOMPLET : "+s.getGroupe().getLabel()+" — "+s.getAutresJury().size()+" membre(s) (min 2)");
         
            long nbProfsAnglais = p.getSoutenances().stream()
                .flatMap(sout -> sout.getJuryComplet().stream())
                .filter(pr -> pr.getSpecialite() == Specialite.ANGLAIS)
                .distinct().count();

            if (s.isAnglais()
                && nbProfsAnglais > 0
                && s.getAutresJury().stream()
                       .noneMatch(pr -> pr.getSpecialite() == Specialite.ANGLAIS))
                errors.add("JURY ANGLAIS INVALIDE : " + s.getGroupe().getLabel()
                    + " — aucun prof d'anglais");
            if(s.getAutresJury().contains(s.getEncadrant()))
                errors.add("ENCADRANT EN DOUBLE : "+s.getEncadrant().getFullName()+" dans "+s.getGroupe().getLabel());
        }
        return errors.isEmpty();
    }
    @Override public List<String> getErrors(){return errors;}
    @Override public String getNom(){return "Conformité du jury";}
}

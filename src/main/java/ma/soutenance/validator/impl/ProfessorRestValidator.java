package ma.soutenance.validator.impl;
import ma.soutenance.model.*;
import ma.soutenance.validator.Validator;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
public class ProfessorRestValidator implements Validator {
    private final List<String> errors=new ArrayList<>();
    @Override public boolean validate(Planning p){
        errors.clear();
        Configuration config = p.getConfiguration();
        LocalTime finMatin = config != null ? config.getHeureFinMatin() : LocalTime.of(12, 0);
        LocalTime debutApm = config != null ? config.getHeureDebutApresMidi() : LocalTime.of(14, 0);

        Map<String,Map<LocalDate,List<Soutenance>>> map=new HashMap<>();
        for(Soutenance s:p.getSoutenances()){
            if(s.getCreneau()==null) continue;
            for(Professeur pr:s.getJuryComplet())
                map.computeIfAbsent(pr.getId(),k->new HashMap<>())
                   .computeIfAbsent(s.getCreneau().getDate(),k->new ArrayList<>()).add(s);
        }
        for(var pe:map.entrySet()) 
        	for(var je:pe.getValue().entrySet()){
        		List<Soutenance> sl=je.getValue().stream()
        				.sorted(Comparator.comparing(s->s.getCreneau().getHeureDebut()))
        				.collect(Collectors.toList());
        		for(int i=0;i<sl.size()-1;i++){
        			LocalTime fin=sl.get(i).getCreneau().getHeureFin();
        			LocalTime debut=sl.get(i+1).getCreneau().getHeureDebut();
        			long gap=Duration.between(fin,debut).toMinutes();
        			boolean pauseDej=!fin.isAfter(finMatin) && !debut.isBefore(debutApm);
        			if(gap>=0&&gap<60&&!pauseDej)
        				errors.add("REPOS INSUFFISANT : "+pe.getKey()+" le "+je.getKey()+" — écart "+gap+"min entre "+fin+" et "+debut);
        		}
        	}
        return errors.isEmpty();
    }
    @Override public List<String> getErrors(){return errors;}
    @Override public String getNom(){return "Temps de repos prof (1h)";}
}

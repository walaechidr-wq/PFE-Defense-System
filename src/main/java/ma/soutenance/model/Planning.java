package ma.soutenance.model;

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Planning complet — v2.0 : compatible FiliereDynamique (getLibelle() existe dans les deux).
 */
public class Planning implements Serializable {

    private static final long serialVersionUID = 1L;

    private String          id;
    private List<Soutenance> soutenances   = new ArrayList<>();
    private Configuration    configuration;
    private LocalDateTime    dateGeneration = LocalDateTime.now();

    public Planning() {
        this.id = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    public Planning(Configuration c) {
        this();
        configuration = c;
    }

    public void addSoutenance(Soutenance s) { if (s != null) soutenances.add(s); }

    public Map<LocalDate, List<Soutenance>> getSoutenancesByDate() {
        return soutenances.stream()
            .filter(s -> s.getCreneau() != null)
            .collect(Collectors.groupingBy(s -> s.getCreneau().getDate()));
    }

    public Map<String, Long> getStatsByFiliere() {
        return soutenances.stream()
            .filter(s -> s.getGroupe() != null && s.getGroupe().getFiliere() != null)
            .collect(Collectors.groupingBy(
                s -> s.getGroupe().getFiliere().getLibelle(), // getLibelle() sur FiliereDynamique
                Collectors.counting()));
    }

    public Map<String, Long> getChargeByEncadrant() {
        return soutenances.stream()
            .filter(s -> s.getEncadrant() != null)
            .collect(Collectors.groupingBy(
                s -> s.getEncadrant().getFullName(), Collectors.counting()));
    }

    public long getNbAnglais() {
        return soutenances.stream().filter(Soutenance::isAnglais).count();
    }

    public String           getId()               { return id; }
    public void             setId(String i)       { id = i; }

    public List<Soutenance> getSoutenances()      { return soutenances; }
    public void             setSoutenances(List<Soutenance> s) { soutenances = s; }

    public Configuration    getConfiguration()    { return configuration; }
    public void             setConfiguration(Configuration c) { configuration = c; }

    public LocalDateTime    getDateGeneration()   { return dateGeneration; }
    public void             setDateGeneration(LocalDateTime d) { dateGeneration = d; }
}
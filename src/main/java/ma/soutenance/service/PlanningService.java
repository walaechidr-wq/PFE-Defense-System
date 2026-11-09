package ma.soutenance.service;
import ma.soutenance.model.*;
import java.util.List;
public interface PlanningService {
    Planning genererPlanning(List<Groupe> groupes);
}

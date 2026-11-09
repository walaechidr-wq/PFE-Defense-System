package ma.soutenance.exception;
public class CapaciteDepasseeException extends SoutenanceException {
    private final int nbSoutenances,capacite;
    public CapaciteDepasseeException(int nb,int cap){
        super(String.format("CAPACITÉ DÉPASSÉE : %d soutenances > %d créneaux disponibles (augmentez les jours ou les salles)",nb,cap));
        this.nbSoutenances=nb;this.capacite=cap;
    }
    public int getNbSoutenances(){return nbSoutenances;}
    public int getCapacite(){return capacite;}
}

package ma.soutenance.config;

import ma.soutenance.exporter.PlanningExporter;
import ma.soutenance.exporter.impl.ExcelPlanningExporter;
import ma.soutenance.exporter.impl.PdfPlanningExporter;
import ma.soutenance.history.HistoryManager;
import ma.soutenance.model.*;
import ma.soutenance.repository.impl.ExcelDataLoader;
import ma.soutenance.repository.impl.ExcelDataLoader.DonneesExcel;
import ma.soutenance.service.PlanningService;
import ma.soutenance.service.impl.AffectationService;
import ma.soutenance.service.impl.ValidationService;
import ma.soutenance.ui.util.ProfesseurColorRegistry;
import ma.soutenance.validator.Validator;
import ma.soutenance.validator.impl.*;

import java.io.File;
import java.util.List;

/**
 * Conteneur DI — Singleton.
 * v2.1 — Chargement Excel optimisé : une seule ouverture du fichier via loadAll().
 */
public class AppContext {

    private static final AppContext INSTANCE = new AppContext();
    public static final String OUTPUT_DIR = "output";

    private String              excelPath;
    private ExcelDataLoader     dataLoader;
    private PlanningService     planningService;
    private ValidationService   validationService;
    private HistoryManager      historyManager;
    private PlanningExporter    pdfExporter;
    private PlanningExporter    excelExporter;

    private List<Etudiant>   etudiants;
    private List<Professeur> professeurs;
    private List<Groupe>     groupes;
    private List<Salle>      salles;
    private Configuration    configuration;
    private List<Filiere>    filieres;

    private boolean initialise = false;

    private AppContext() {}

    public static AppContext get() { return INSTANCE; }

    /** Initialisation au démarrage. */
    public void init(String excelPath) {
        this.excelPath = excelPath;
        charger(excelPath);
    }

    /** Réinitialisation depuis un nouveau fichier (bouton Importer). */
    public void reinit(String newPath) {
        this.excelPath = newPath;
        ProfesseurColorRegistry.reset();
        charger(newPath);
    }

    /**
     * Chargement optimisé — ouvre le fichier Excel UNE SEULE FOIS
     * pour toutes les feuilles. 5x plus rapide qu'avant.
     */
    private void charger(String path) {
        this.dataLoader = new ExcelDataLoader(path);

        // ── Lecture unique du fichier Excel ───────────────────────────────────
        DonneesExcel donnees = dataLoader.loadAll();

        // ── Alimentation des champs ───────────────────────────────────────────
        this.filieres      = donnees.filieres;
        this.etudiants     = donnees.etudiants;
        this.professeurs   = donnees.professeurs;
        this.salles        = donnees.salles;
        this.configuration = donnees.configuration;
        this.groupes       = donnees.groupes;

        // ── Enregistrer les couleurs des professeurs ──────────────────────────
        ProfesseurColorRegistry.get().enregistrerTous(professeurs);

        // ── Instancier les services ───────────────────────────────────────────
        List<Validator> validators = List.of(
            new RoomOverlapValidator(),
            new ProfessorBusyValidator(),
            new ProfessorRestValidator(),
            new JuryConformiteValidator()
        );

        this.planningService   = new AffectationService(professeurs, salles, configuration);
        this.validationService = new ValidationService(validators);

        if (this.historyManager == null) this.historyManager = new HistoryManager();
        this.pdfExporter   = new PdfPlanningExporter();
        this.excelExporter = new ExcelPlanningExporter();

        new File(OUTPUT_DIR).mkdirs();
        this.initialise = true;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────
    public boolean           isInitialise()         { return initialise; }
    public String            getExcelPath()         { return excelPath; }
    public ExcelDataLoader   getDataLoader()        { return dataLoader; }
    public PlanningService   getPlanningService()   { return planningService; }
    public ValidationService getValidationService() { return validationService; }
    public HistoryManager    getHistoryManager()    { return historyManager; }
    public PlanningExporter  getPdfExporter()       { return pdfExporter; }
    public PlanningExporter  getExcelExporter()     { return excelExporter; }
    public List<Etudiant>    getEtudiants()         { return etudiants; }
    public List<Professeur>  getProfesseurs()       { return professeurs; }
    public List<Groupe>      getGroupes()           { return groupes; }
    public List<Salle>       getSalles()            { return salles; }
    public Configuration     getConfiguration()     { return configuration; }
    public List<Filiere>     getFilieres()          { return filieres; }
}
package ma.soutenance.ui;

import ma.soutenance.config.AppContext;
import ma.soutenance.model.Planning;
import ma.soutenance.service.impl.ValidationService;
import ma.soutenance.ui.component.RoundedPanel;
import ma.soutenance.ui.component.SidebarPanel;
import ma.soutenance.ui.component.StatusBar;
import ma.soutenance.ui.panel.*;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.SwingUtil;
import ma.soutenance.exporter.impl.PvGeneratorService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Desktop;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * Fenêtre principale — v2.0
 * L'utilisateur importe le fichier Excel via le bouton « Importer Excel ».
 * La génération du planning se lance automatiquement après l'import.
 */
public class MainFrame extends JFrame implements SidebarPanel.NavListener {

    private final AppContext ctx = AppContext.get();
    private String vueApresGeneration = "AFFECTATION";

    private final SidebarPanel     sidebar      = new SidebarPanel();
    private final StatusBar        statusBar    = new StatusBar();
    private final JPanel           contentArea  = new JPanel(new CardLayout());

    private final DashboardPanel   pDashboard   = new DashboardPanel();
    private final AffectationPanel pAffectation = new AffectationPanel();
    private final PlanningPanel    pPlanning    = new PlanningPanel();
    private final ConformitePanel  pConformite  = new ConformitePanel();
    private final HistoriquePanel  pHistorique  = new HistoriquePanel();

    private Planning currentPlanning;

    private final JButton btnImporter = AppTheme.btnPrimary("Importer Excel", Icons.UPLOAD);
    private RoundedPanel statusChip;

    public MainFrame() {
        super("Gestion des Soutenances PFE — ENSA UAE");
        buildUI();
        showView("DASHBOARD");
        // Pas de génération automatique — attend l'import Excel
    }

    // ── Construction UI ───────────────────────────────────────────────────────
    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setIconImage(Icons.appImage(64));

        sidebar.setNavListener(this);

        contentArea.setBackground(AppTheme.BG_APP);
        contentArea.add(pDashboard,   "DASHBOARD");
        contentArea.add(pAffectation, "AFFECTATION");
        contentArea.add(pPlanning,    "PLANNING");
        contentArea.add(pConformite,  "CONFORMITE");
        contentArea.add(pHistorique,  "HISTORIQUE");

        JPanel center = new JPanel(new BorderLayout());
        center.add(buildTopBar(),  BorderLayout.NORTH);
        center.add(contentArea,    BorderLayout.CENTER);
        center.add(statusBar,      BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(center,  BorderLayout.CENTER);

        // Adaptation à la largeur : navigation réduite aux icônes sur les fenêtres étroites
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { sidebar.setCompact(getWidth() < 1180); }
        });
        sidebar.setCompact(getWidth() < 1180);
    }

    /** Déclenche l'import Excel (même action que le bouton de la barre supérieure). */
    public void importerExcel() { btnImporter.doClick(); }

    // ── Barre supérieure avec bouton Importer ─────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(0, AppTheme.HEADER_H));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
            new EmptyBorder(0, AppTheme.PAD + 2, 0, AppTheme.PAD + 2)));

        JLabel lblMsg = new JLabel("Commencez par importer votre fichier Excel PFE",
            Icons.icon(Icons.UPLOAD, 18, AppTheme.BURGUNDY), SwingConstants.LEADING);
        lblMsg.setIconTextGap(10);
        lblMsg.setFont(AppTheme.F_BOLD);
        lblMsg.setForeground(AppTheme.BURGUNDY);

        statusChip = new RoundedPanel(new BorderLayout(), 12, AppTheme.PRIMARY_LIGHT, null, false);
        statusChip.setBorder(new EmptyBorder(9, 14, 9, 18));
        statusChip.add(lblMsg, BorderLayout.CENTER);

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        left.add(statusChip);
        bar.add(left, BorderLayout.WEST);

        btnImporter.addActionListener(e -> importerFichierExcel(lblMsg));

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(btnImporter);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    /** Passe la pastille d'état en « fichier chargé » (vert sobre, icône de validation). */
    private void markImportLoaded(JLabel lblMsg) {
        lblMsg.setIcon(Icons.icon(Icons.CHECK_CIRCLE, 18, AppTheme.SUCCESS));
        lblMsg.setForeground(AppTheme.SUCCESS);
        if (statusChip != null) statusChip.setFill(AppTheme.SUCCESS_LIGHT);
    }

    // ── Import du fichier Excel ───────────────────────────────────────────────
    private void importerFichierExcel(JLabel lblMsg) {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Sélectionner le fichier Excel PFE");
        fc.setFileFilter(new FileNameExtensionFilter(
            "Fichier Excel (*.xlsx, *.xls)", "xlsx", "xls"));
        fc.setAcceptAllFileFilterUsed(false);

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        // Confirmation si un planning est déjà généré
        if (currentPlanning != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Charger un nouveau fichier remplacera le planning actuel.\nContinuer ?",
                "Confirmation", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        File fichier = fc.getSelectedFile();
        statusBar.busy("Chargement de " + fichier.getName() + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (!AppContext.get().isInitialise())
                    AppContext.get().init(fichier.getAbsolutePath());
                else
                    AppContext.get().reinit(fichier.getAbsolutePath());
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    // Mettre à jour le label avec les infos du fichier chargé
                    lblMsg.setText("Fichier chargé : " + fichier.getName()
                        + "  |  " + ctx.getGroupes().size() + " groupes"
                        + "  |  " + ctx.getProfesseurs().size() + " profs");
                    markImportLoaded(lblMsg);

                    statusBar.ok("Fichier chargé — lancement de l'affectation...");
                    vueApresGeneration = "AFFECTATION";
                    lancerGeneration();

                } catch (Exception ex) {
                    statusBar.err("Erreur : " + ex.getMessage());
                    SwingUtil.error(MainFrame.this,
                        "Erreur de chargement", ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @Override
    public void onNavigate(String viewId) {
        // Vérifier que les données sont chargées avant de naviguer
        if (!ctx.isInitialise() && !"DASHBOARD".equals(viewId)) {
            JOptionPane.showMessageDialog(this,
                "Veuillez d'abord importer un fichier Excel.",
                "Aucune donnée", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("AFFECTATION".equals(viewId) || "PLANNING".equals(viewId)) {
            vueApresGeneration = viewId;
            lancerGeneration();
            return;
        }
        if ("GENERER_PV".equals(viewId)) {
            genererPVs();
            return;
        }
        showView(viewId);
    }

    private void showView(String viewId) {
        ((CardLayout) contentArea.getLayout()).show(contentArea, viewId);
        sidebar.setActive(viewId);
        if ("HISTORIQUE".equals(viewId)) pHistorique.charger();
    }

    // ── Génération du planning ────────────────────────────────────────────────
    private void lancerGeneration() {
        if (!ctx.isInitialise()) {
            JOptionPane.showMessageDialog(this,
                "Veuillez d'abord importer un fichier Excel.",
                "Aucune donnée", JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusBar.busy("Génération de l'affectation en cours...");

        SwingWorker<Planning, Void> worker = new SwingWorker<>() {
            @Override
            protected Planning doInBackground() throws Exception {
                Planning planning = ctx.getPlanningService()
                    .genererPlanning(ctx.getGroupes());

                ValidationService.ValidationResult res =
                    ctx.getValidationService().valider(planning);

                try {
                    ctx.getHistoryManager().sauvegarder(
                        planning, null, null,
                        res.isValide(), res.getResumeCourt());
                } catch (Exception ex) {
                    System.err.println("[Historique] " + ex.getMessage());
                }
                return planning;
            }
         
            @Override
            protected void done() {
                try {
                    currentPlanning = get();

                    pAffectation.setPlanning(currentPlanning);
                    pPlanning.setPlanning(currentPlanning);
                    pConformite.setPlanning(currentPlanning);
                    pDashboard.refresh();

                    int nb = currentPlanning.getSoutenances().size();
                    String d = currentPlanning.getDateGeneration()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                    statusBar.ok("Planning généré — " + nb + " soutenances  |  " + d);
                    showView(vueApresGeneration);

                } catch (Exception ex) {
                    statusBar.err("Erreur : " + ex.getMessage());
                    SwingUtil.error(MainFrame.this,
                        "Erreur de génération", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ── Génération des PVs ────────────────────────────────────────────────────
    private void genererPVs() {
        if (currentPlanning == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez d'abord générer un planning.",
                "Aucun planning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusBar.busy("Génération des PVs en cours...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new PvGeneratorService().generateAllPV(
                    currentPlanning, AppContext.OUTPUT_DIR + "/PV");
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    statusBar.ok("PVs générés → " + AppContext.OUTPUT_DIR + "/PV");
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "PVs générés dans : " + AppContext.OUTPUT_DIR + "/PV",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                    Desktop.getDesktop().open(
                        new java.io.File(AppContext.OUTPUT_DIR + "/PV"));
                } catch (Exception ex) {
                    statusBar.err("Erreur PVs : " + ex.getMessage());
                    SwingUtil.error(MainFrame.this, "Erreur PVs", ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Exports ───────────────────────────────────────────────────────────────
    public void exporterAffectationPdf() {
        if (currentPlanning == null) { noPlanning(); return; }
        File dest = choisirFichier("Affectation PDF",
            "affectation_" + timestamp() + ".pdf", "pdf", "Fichier PDF (*.pdf)");
        if (dest == null) return;
        lancerExport("Affectation PDF",
            () -> ctx.getPdfExporter().export(currentPlanning, dest.getAbsolutePath()));
    }

    public void exporterAffectationExcel() {
        if (currentPlanning == null) { noPlanning(); return; }
        File dest = choisirFichier("Affectation Excel",
            "affectation_" + timestamp() + ".xlsx", "xlsx", "Fichier Excel (*.xlsx)");
        if (dest == null) return;
        lancerExport("Affectation Excel",
            () -> ctx.getExcelExporter().export(currentPlanning, dest.getAbsolutePath()));
    }

    public void exporterPlanningPdf() {
        if (currentPlanning == null) { noPlanning(); return; }
        File dest = choisirFichier("Planning PDF",
            "planning_" + timestamp() + ".pdf", "pdf", "Fichier PDF (*.pdf)");
        if (dest == null) return;
        lancerExport("Planning PDF",
            () -> ctx.getPdfExporter().export(currentPlanning, dest.getAbsolutePath()));
    }

    public void exporterPlanningExcel() {
        if (currentPlanning == null) { noPlanning(); return; }
        File dest = choisirFichier("Planning Excel",
            "planning_" + timestamp() + ".xlsx", "xlsx", "Fichier Excel (*.xlsx)");
        if (dest == null) return;
        lancerExport("Planning Excel",
            () -> ctx.getExcelExporter().export(currentPlanning, dest.getAbsolutePath()));
    }

    private void lancerExport(String label, ExportTask task) {
        statusBar.busy("Export " + label + " en cours...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                task.run(); return null;
            }
            @Override protected void done() {
                try {
                    get();
                    statusBar.ok("Export " + label + " terminé");
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Fichier exporté avec succès.",
                        "Export " + label, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusBar.err("Erreur export : " + ex.getMessage());
                    SwingUtil.error(MainFrame.this, "Erreur export", ex.getMessage());
                }
            }
        }.execute();
    }

    private File choisirFichier(String titre, String defaut, String ext, String desc) {
        JFileChooser fc = new JFileChooser(new File(AppContext.OUTPUT_DIR));
        fc.setDialogTitle("Exporter — " + titre);
        fc.setSelectedFile(new File(AppContext.OUTPUT_DIR, defaut));
        fc.setFileFilter(new FileNameExtensionFilter(desc, ext));
        int r = fc.showSaveDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        if (!f.getName().endsWith("." + ext))
            f = new File(f.getAbsolutePath() + "." + ext);
        return f;
    }

    private void noPlanning() {
        JOptionPane.showMessageDialog(this,
            "Aucun planning disponible. Importez d'abord un fichier Excel.",
            "Aucun planning", JOptionPane.WARNING_MESSAGE);
    }

    private String timestamp() {
        return java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    @FunctionalInterface
    private interface ExportTask { void run() throws Exception; }
}
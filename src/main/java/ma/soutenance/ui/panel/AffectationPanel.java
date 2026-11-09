package ma.soutenance.ui.panel;

import ma.soutenance.config.AppContext;
import ma.soutenance.model.*;
import ma.soutenance.ui.MainFrame;
import ma.soutenance.ui.component.TableKit;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.ProfesseurColorRegistry;
import ma.soutenance.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panneau Affectation des Encadrants.
 * v2.0 — FiliereDynamique + code couleur encadrant dans le tableau.
 */
public class AffectationPanel extends JPanel {

    private final JTabbedPane tabs    = new JTabbedPane();
    private final JLabel      lblInfo = AppTheme.muted("Aucun planning généré.");

    private DefaultTableModel modelGroupes;
    private DefaultTableModel modelCharge;
    private Planning          currentPlanning;

    private final JButton btnExportPdf   = AppTheme.btnSecondary("Affectation PDF", Icons.FILE_TEXT);
    private final JButton btnExportExcel = AppTheme.btnSecondary("Affectation Excel", Icons.TABLE);

    private final ProfesseurColorRegistry colorRegistry = ProfesseurColorRegistry.get();

    // Index ligne → Professeur pour le renderer couleur
    private final Map<Integer, Professeur> encadrantParLigne = new HashMap<>();

    public AffectationPanel() {
        setLayout(new BorderLayout(0, AppTheme.GAP));
        setBackground(AppTheme.BG_APP);
        setBorder(new EmptyBorder(AppTheme.PAD, AppTheme.PAD, AppTheme.PAD, AppTheme.PAD));

        lblInfo.setBorder(new EmptyBorder(10, 4, 0, 0));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
        add(lblInfo,       BorderLayout.SOUTH);

        btnExportPdf.setEnabled(false);
        btnExportExcel.setEnabled(false);

        btnExportPdf.addActionListener(e -> getMainFrame().exporterAffectationPdf());
        btnExportExcel.addActionListener(e -> getMainFrame().exporterAffectationExcel());
    }

    private JPanel buildHeader() {
        return SwingUtil.pageHeader("Affectation des Encadrants",
            "Répartition équitable — load-balancing ⌊M⌋/⌈M⌉.",
            Icons.USER_CHECK,
            SwingUtil.actionBar("Exporter :", btnExportPdf, btnExportExcel));
    }

    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // ── Onglet 1 : Groupes & Encadrants ──────────────────────────────────
        String[] colsG = {"Groupe(s)", "Filière", "Langue", "Encadrant", "Spécialité", "Nb étudiants"};
        modelGroupes = new DefaultTableModel(colsG, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tableGroupes = new JTable(modelGroupes);
        SwingUtil.styleTable(tableGroupes);

        // Colonnes visuelles : langue (pastille) et encadrant (chip colorée par professeur)
        SwingUtil.pillColumn(tableGroupes, 2, TableKit.langueStyle(), false);
        tableGroupes.getColumnModel().getColumn(3).setCellRenderer(
            new TableKit.PillRenderer(null, false, row -> {
                Professeur prof = encadrantParLigne.get(row);
                return prof != null ? colorRegistry.getCouleur(prof) : null;
            }));

        int[] wg = {260, 200, 90, 190, 120, 120};
        for (int i = 0; i < wg.length; i++) tableGroupes.getColumnModel().getColumn(i).setPreferredWidth(wg[i]);

        // ── Onglet 2 : Charge par Professeur ─────────────────────────────────
        String[] colsC = {"Professeur", "Spécialité", "Nb encadrements", "Groupes encadrés"};
        modelCharge = new DefaultTableModel(colsC, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tableCharge = new JTable(modelCharge);
        SwingUtil.styleTable(tableCharge);

        tabs.addTab("Groupes & Encadrants",  SwingUtil.scrollPane(tableGroupes, "Aucune affectation — importez un fichier Excel"));
        tabs.addTab("Charge par Professeur", SwingUtil.scrollPane(tableCharge,  "Aucune donnée de charge à afficher"));
        tabs.setFont(AppTheme.F_BODY);
        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    public void setPlanning(Planning planning) {
        this.currentPlanning = planning;
        modelGroupes.setRowCount(0);
        modelCharge.setRowCount(0);
        encadrantParLigne.clear();

        if (planning == null || planning.getSoutenances().isEmpty()) {
            lblInfo.setText("Aucun planning généré.");
            btnExportPdf.setEnabled(false);
            btnExportExcel.setEnabled(false);
            return;
        }

        List<Soutenance> soutenances = planning.getSoutenances();
        int row = 0;
        for (Soutenance s : soutenances) {
            Groupe g   = s.getGroupe();
            Professeur enc = s.getEncadrant();
            if (g == null) continue;

            // v2.0 : getFiliere().getLibelle() sur FiliereDynamique
            String filLib = g.getFiliere() != null ? g.getFiliere().getLibelle() : "?";

            modelGroupes.addRow(new Object[]{
                g.getLabel(),
                filLib,
                g.isAnglais() ? "Anglais" : "Français",
                enc != null ? enc.getFullName() : "Non affecté",
                enc != null && enc.getSpecialite() != null ? enc.getSpecialite().getLibelle() : "?",
                g.getEtudiants().size()
            });

            if (enc != null) encadrantParLigne.put(row, enc);
            row++;
        }

        Map<Professeur, List<Soutenance>> parProf = soutenances.stream()
            .filter(s -> s.getEncadrant() != null)
            .collect(Collectors.groupingBy(Soutenance::getEncadrant));

        parProf.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .forEach(entry -> {
                String groupesLabel = entry.getValue().stream()
                    .map(s -> s.getGroupe() != null ? s.getGroupe().getLabel() : "?")
                    .collect(Collectors.joining(" | "));
                modelCharge.addRow(new Object[]{
                    entry.getKey().getFullName(),
                    entry.getKey().getSpecialite() != null
                        ? entry.getKey().getSpecialite().getLibelle() : "?",
                    entry.getValue().size(),
                    groupesLabel
                });
            });

        AppContext ctx = AppContext.get();
        Set<Professeur> affectes = parProf.keySet();
        ctx.getProfesseurs().stream()
            .filter(p -> !affectes.contains(p))
            .forEach(p -> modelCharge.addRow(new Object[]{
                p.getFullName(),
                p.getSpecialite() != null ? p.getSpecialite().getLibelle() : "?",
                0, "—"
            }));

        lblInfo.setText(soutenances.size() + " soutenance(s)  |  "
            + parProf.size() + " encadrant(s)  |  "
            + (ctx.getProfesseurs().size() - parProf.size()) + " non affecté(s)");

        btnExportPdf.setEnabled(true);
        btnExportExcel.setEnabled(true);
    }

    private MainFrame getMainFrame() {
        return (MainFrame) SwingUtilities.getWindowAncestor(this);
    }
}
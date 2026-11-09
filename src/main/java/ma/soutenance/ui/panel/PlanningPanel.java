package ma.soutenance.ui.panel;

import ma.soutenance.model.*;
import ma.soutenance.ui.MainFrame;
import ma.soutenance.ui.component.TableKit;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Panneau d'affichage du planning généré.
 * v1.2 — Boutons export séparés : Planning PDF | Planning Excel
 */
public class PlanningPanel extends JPanel {

    private Planning planning;
    private final JTabbedPane tabs     = new JTabbedPane();
    private final JLabel      lblTotal = AppTheme.muted("");

    // Boutons export
    private final JButton btnExportPdf   = AppTheme.btnSecondary("Planning PDF", Icons.FILE_TEXT);
    private final JButton btnExportExcel = AppTheme.btnSecondary("Planning Excel", Icons.TABLE);

    public PlanningPanel() {
        setLayout(new BorderLayout(0, AppTheme.GAP));
        setBackground(AppTheme.BG_APP);
        setBorder(new EmptyBorder(AppTheme.PAD, AppTheme.PAD, AppTheme.PAD, AppTheme.PAD));

        add(buildHeader(), BorderLayout.NORTH);
        add(tabs,          BorderLayout.CENTER);

        // Désactivés jusqu'à génération
        btnExportPdf.setEnabled(false);
        btnExportExcel.setEnabled(false);

        // Actions — délèguent à MainFrame
        btnExportPdf.addActionListener(e -> getMainFrame().exporterPlanningPdf());
        btnExportExcel.addActionListener(e -> getMainFrame().exporterPlanningExcel());

        showEmpty();
    }

    //  En-tête avec boutons export
    private JPanel buildHeader() {
        return SwingUtil.pageHeader("Planning des Soutenances", lblTotal, Icons.CALENDAR,
            SwingUtil.actionBar("Exporter :", btnExportPdf, btnExportExcel));
    }

    //Mise à jour des données 
    public void setPlanning(Planning p) {
        this.planning = p;
        tabs.removeAll();

        if (p == null || p.getSoutenances().isEmpty()) {
            showEmpty();
            btnExportPdf.setEnabled(false);
            btnExportExcel.setEnabled(false);
            return;
        }

        lblTotal.setText(p.getSoutenances().size() + " soutenances  •  "
            + p.getNbAnglais() + " en anglais  •  généré le "
            + p.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Onglet "Tout"
        tabs.addTab("Tout (" + p.getSoutenances().size() + ")",
            buildTable(p.getSoutenances()));

        // Onglet par journée
        new TreeMap<>(p.getSoutenancesByDate()).forEach((date, list) -> {
            String label = date.format(DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH));
            tabs.addTab(label + " (" + list.size() + ")", buildTable(list));
        });

        tabs.setFont(AppTheme.F_BODY);

        // Activer les boutons export
        btnExportPdf.setEnabled(true);
        btnExportExcel.setEnabled(true);
    }

    //  Tableau des soutenances 
    private JScrollPane buildTable(List<Soutenance> list) {
        String[] cols = {"Heure", "Groupe(s)", "Filière", "Langue", "Encadrant", "Jury (membres)", "Salle"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        for (Soutenance s : list) {
            String heure  = s.getCreneau() != null
                ? s.getCreneau().getHeureDebut().format(tf) + "-" + s.getCreneau().getHeureFin().format(tf)
                : "?";
            String groupe  = s.getGroupe()    != null ? s.getGroupe().getLabel()          : "?";
            String filiere = s.getGroupe()    != null ? s.getGroupe().getFiliere().name() : "?";
            String langue  = s.isAnglais() ? "Anglais" : "Français";
            String enc     = s.getEncadrant() != null ? s.getEncadrant().getFullName()     : "?";
            String jury    = s.getAutresJury().stream()
                              .map(Professeur::getFullName)
                              .reduce("", (a, b) -> a.isEmpty() ? b : a + " | " + b);
            String salle   = s.getSalleNom();

            model.addRow(new Object[]{heure, groupe, filiere, langue, enc, jury, salle});
        }

        JTable table = new JTable(model);
        SwingUtil.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(125);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(105);
        table.getColumnModel().getColumn(4).setPreferredWidth(190);
        table.getColumnModel().getColumn(5).setPreferredWidth(250);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);

        // Langue en pastille ; encadrant en chip colorée par professeur
        SwingUtil.pillColumn(table, 3, TableKit.langueStyle(), false);
        table.getColumnModel().getColumn(4).setCellRenderer(
            new TableKit.PillRenderer(null, false, row -> {
                if (row < 0 || row >= list.size()) return null;
                Professeur prof = list.get(row).getEncadrant();
                return prof != null
                    ? ma.soutenance.ui.util.ProfesseurColorRegistry.get().getCouleur(prof) : null;
            }));

        return SwingUtil.scrollPane(table, "Aucune soutenance à afficher");
    }

    private void showEmpty() {
        tabs.removeAll();
        tabs.addTab("Planning", SwingUtil.emptyState(Icons.CALENDAR, "Aucun planning généré",
            "Importez un fichier Excel PFE : le planning est généré automatiquement après l'import.", null));
        lblTotal.setText("");
    }

    private MainFrame getMainFrame() {
        return (MainFrame) SwingUtilities.getWindowAncestor(this);
    }
}
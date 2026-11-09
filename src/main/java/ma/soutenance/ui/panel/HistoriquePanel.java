package ma.soutenance.ui.panel;

import ma.soutenance.config.AppContext;
import ma.soutenance.history.HistoryManager;
import ma.soutenance.ui.component.TableKit;
import ma.soutenance.ui.component.WrapLayout;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panneau Historique — affiche toutes les sessions de génération.
 *
 * Correction v2 — lazy loading :
 * Le HistoryManager n'est plus récupéré à l'initialisation du champ
 * (ce qui provoquait un NPE si AppContext n'était pas encore initialisé),
 * mais via la méthode privée {@link #hm()} appelée au moment de l'usage.
 */
public class HistoriquePanel extends JPanel {

    // ── PLUS de "= AppContext.get().getHistoryManager()" ici ─────────────────
    // Le hm est résolu lazily via hm() pour éviter le NPE au démarrage.

    private final DefaultTableModel model;
    private final JTable table;
    private final JButton btnExcel, btnPdf, btnSupprimer, btnVider, btnDossier, btnRefresh;
    private final JLabel lblInfo;
    private List<HistoryManager.EntreeInfo> entries;

    public HistoriquePanel() {
        setLayout(new BorderLayout(0, AppTheme.GAP));
        setBackground(AppTheme.BG_APP);
        setBorder(new EmptyBorder(AppTheme.PAD, AppTheme.PAD, AppTheme.PAD, AppTheme.PAD));

        model = new DefaultTableModel(
            new String[]{"ID Planning","Date génération","Soutenances","Jours","Date début","Conforme","Fichiers"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table        = new JTable(model);
        btnExcel     = AppTheme.btnPrimary("Affectations (Excel)", Icons.TABLE);
        btnPdf       = AppTheme.btnPrimary("Planning (PDF)", Icons.FILE_TEXT);
        btnSupprimer = AppTheme.btnDanger("Supprimer", Icons.TRASH);
        btnVider     = AppTheme.btnDanger("Vider tout", Icons.TRASH);
        btnDossier   = AppTheme.btnSecondary("Dossier historique", Icons.FOLDER);
        btnRefresh   = AppTheme.btnSecondary("Actualiser", Icons.REFRESH);
        lblInfo      = AppTheme.muted("Importez un fichier Excel pour commencer.");

        btnExcel.setEnabled(false);
        btnPdf.setEnabled(false);
        btnSupprimer.setEnabled(false);
        btnVider.setEnabled(false);

        wireActions();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        // NE PAS appeler charger() ici — AppContext pas encore initialisé
        // charger() sera appelé par MainFrame.showView("HISTORIQUE")
    }

    // ── Lazy accessor — résout le HistoryManager au moment de l'appel ─────────
    /**
     * Retourne le {@link HistoryManager} depuis AppContext.
     * Retourne {@code null} si AppContext n'est pas encore initialisé
     * (cas du démarrage avant import Excel).
     */
    private HistoryManager hm() {
        if (!AppContext.get().isInitialise()) return null;
        return AppContext.get().getHistoryManager();
    }

    // ── Construction UI ───────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, AppTheme.GAP, 0));
        p.add(SwingUtil.pageHeader("Historique des Plannings",
            "Tous les fichiers d'affectations et plannings générés depuis le début du système.",
            Icons.HISTORY, null), BorderLayout.NORTH);

        JPanel btns = new JPanel(new WrapLayout(FlowLayout.LEFT, AppTheme.GAP - 4, 6));
        btns.setOpaque(false);
        for (JButton b : new JButton[]{btnExcel, btnPdf, btnDossier, btnRefresh}) btns.add(b);
        btns.add(Box.createHorizontalStrut(16));
        for (JButton b : new JButton[]{btnSupprimer, btnVider}) btns.add(b);
        p.add(btns, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        SwingUtil.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] w = {115, 155, 85, 55, 95, 75, 190};
        for (int i = 0; i < w.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = table.getSelectedRow() >= 0;
            btnExcel.setEnabled(sel);
            btnPdf.setEnabled(sel);
            btnSupprimer.setEnabled(sel);
            if (sel && entries != null && table.getSelectedRow() < entries.size()) {
                HistoryManager.EntreeInfo ei = entries.get(table.getSelectedRow());
                lblInfo.setText("Validation : " + ei.resumeValidation);
            }
        });

        // Colonnes visuelles : conformité et fichiers disponibles en pastilles
        SwingUtil.pillColumn(table, 5, TableKit.conformeStyle(), false);
        SwingUtil.pillColumn(table, 6, TableKit.fichiersStyle(), true);

        lblInfo.setBorder(new EmptyBorder(4, 4, 0, 0));
        p.add(SwingUtil.scrollPane(table, "Aucune entrée dans l'historique"), BorderLayout.CENTER);
        p.add(lblInfo, BorderLayout.SOUTH);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void wireActions() {
        btnExcel.addActionListener(e -> ouvrir(true));
        btnPdf.addActionListener(e -> ouvrir(false));
        btnRefresh.addActionListener(e -> charger());

        btnDossier.addActionListener(e -> {
            HistoryManager h = hm();
            if (h == null) { notPret(); return; }
            try { java.awt.Desktop.getDesktop().open(new File(h.getHistoRootPath())); }
            catch (Exception ex) { SwingUtil.info(this, "Dossier", h.getHistoRootPath()); }
        });

        btnSupprimer.addActionListener(e -> {
            HistoryManager h = hm();
            if (h == null) { notPret(); return; }
            int row = table.getSelectedRow();
            if (row < 0) return;
            if (!SwingUtil.confirm(this, "Supprimer", "Supprimer cette entrée de l'historique ?")) return;
            try { h.supprimer(entries.get(row).id); charger(); }
            catch (Exception ex) { SwingUtil.error(this, "Erreur", ex.getMessage()); }
        });

        btnVider.addActionListener(e -> {
            HistoryManager h = hm();
            if (h == null) { notPret(); return; }
            if (!SwingUtil.confirm(this, "Vider", "Supprimer toutes les entrées de l'historique ?")) return;
            try { h.viderTout(); charger(); }
            catch (Exception ex) { SwingUtil.error(this, "Erreur", ex.getMessage()); }
        });
    }

    private void ouvrir(boolean excel) {
        int row = table.getSelectedRow();
        if (row < 0 || entries == null) return;
        HistoryManager.EntreeInfo ei = entries.get(row);
        String chemin = excel ? ei.cheminExcel : ei.cheminPdf;
        if (chemin == null || chemin.isBlank()) {
            SwingUtil.warn(this, "Fichier absent", "Ce fichier n'est pas disponible pour cette entrée.");
            return;
        }
        File f = new File(chemin);
        if (!f.exists()) {
            SwingUtil.warn(this, "Introuvable", "Fichier introuvable :\n" + chemin);
            return;
        }
        try { java.awt.Desktop.getDesktop().open(f); }
        catch (Exception ex) { SwingUtil.error(this, "Erreur ouverture", ex.getMessage()); }
    }

    // ── Chargement public ─────────────────────────────────────────────────────

    /**
     * Recharge les entrées depuis l'historique.
     * Appelé par MainFrame.showView("HISTORIQUE") — à ce stade AppContext
     * est forcément initialisé (la navigation est bloquée sinon).
     * Affiche un message neutre si ce n'est pas encore le cas.
     */
    public void charger() {
        HistoryManager h = hm();

        // AppContext pas encore initialisé (avant premier import)
        if (h == null) {
            model.setRowCount(0);
            lblInfo.setText("Aucune donnée — importez un fichier Excel pour commencer.");
            btnVider.setEnabled(false);
            return;
        }

        model.setRowCount(0);
        entries = h.listerEntrees();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (HistoryManager.EntreeInfo ei : entries) {
            boolean hasExcel = ei.cheminExcel != null && !ei.cheminExcel.isBlank()
                && new File(ei.cheminExcel).exists();
            boolean hasPdf = ei.cheminPdf != null && !ei.cheminPdf.isBlank()
                && new File(ei.cheminPdf).exists();
            String fichiers = ((hasExcel ? " Excel " : "") + (hasPdf ? " PDF" : "")).strip();

            model.addRow(new Object[]{
                ei.id,
                ei.dateGeneration != null ? ei.dateGeneration.format(fmt) : "?",
                ei.nbSoutenances,
                ei.nbJours,
                ei.dateDebut != null ? ei.dateDebut : "?",
                ei.conforme ? "Oui" : "Non",
                fichiers.isBlank() ? "Aucun" : fichiers
            });
        }

        lblInfo.setText(entries.size() + " entrée(s)  |  Dossier : " + h.getHistoRootPath());
        btnVider.setEnabled(!entries.isEmpty());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void notPret() {
        SwingUtil.warn(this, "Non disponible",
            "Importez d'abord un fichier Excel pour accéder à l'historique.");
    }
}
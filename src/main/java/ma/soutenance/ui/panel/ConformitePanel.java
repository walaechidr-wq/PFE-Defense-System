package ma.soutenance.ui.panel;

import ma.soutenance.config.AppContext;
import ma.soutenance.model.Planning;
import ma.soutenance.service.impl.ValidationService;
import ma.soutenance.ui.component.RoundedPanel;
import ma.soutenance.ui.component.RoundedScrollPane;
import ma.soutenance.ui.component.TableKit;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panneau Vérificateur de Conformité.
 * Lance tous les Validators sur le planning courant et affiche les résultats.
 */
public class ConformitePanel extends JPanel {

    private final AppContext ctx = AppContext.get();
    private Planning planning;

    private final JLabel    lblStatut  = new JLabel("En attente d'un planning...");
    private final JLabel    lblDetails = AppTheme.muted("");
    private final DefaultTableModel errModel;
    private final JButton   btnScan;
    private RoundedPanel    banner;
    private final JLabel    bannerIcon = new JLabel();
    private RoundedScrollPane errScroll;

    public ConformitePanel() {
        setLayout(new BorderLayout(0, AppTheme.GAP));
        setBackground(AppTheme.BG_APP);
        setBorder(new EmptyBorder(AppTheme.PAD, AppTheme.PAD, AppTheme.PAD, AppTheme.PAD));

        // Table erreurs
        errModel = new DefaultTableModel(new String[]{"Règle","Description de l'erreur"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        btnScan  = AppTheme.btnPrimary("Lancer la vérification", Icons.PLAY);
        btnScan.setEnabled(false);
        btnScan.addActionListener(e -> lancerVerification());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    public void setPlanning(Planning p) {
        this.planning = p;
        btnScan.setEnabled(p != null);
        if (p == null) {
            lblStatut.setText("En attente d'un planning...");
            lblStatut.setForeground(AppTheme.TEXT_SECONDARY);
            lblDetails.setText("");
            errModel.setRowCount(0);
            setBanner(0);
            errScroll.setEmptyText("Importez un fichier Excel pour lancer une vérification");
        } else {
            lblStatut.setText("Planning chargé — " + p.getSoutenances().size() + " soutenances. Cliquez sur « Lancer ».");
            lblStatut.setForeground(AppTheme.TEXT_PRIMARY);
            setBanner(0);
            errScroll.setEmptyText("Lancez la vérification pour afficher les résultats");
        }
    }

    // Header
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, AppTheme.GAP - 4, 0));
        p.add(SwingUtil.pageHeader("Vérificateur de Conformité",
                "Contrôle automatique des règles de planification.", Icons.SHIELD_CHECK, btnScan),
              BorderLayout.NORTH);

        // Bandeau d'état : neutre / conforme / erreurs
        banner = new RoundedPanel(new BorderLayout(14, 0), AppTheme.R_MD, AppTheme.BG_CARD, AppTheme.BORDER, false);
        banner.setBorder(new EmptyBorder(14, 18, 14, 18));
        lblStatut.setFont(AppTheme.F_SUBTITLE);
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.add(Box.createVerticalGlue());
        txt.add(lblStatut);
        txt.add(lblDetails);
        txt.add(Box.createVerticalGlue());
        banner.add(bannerIcon, BorderLayout.WEST);
        banner.add(txt, BorderLayout.CENTER);
        setBanner(0);
        p.add(banner, BorderLayout.CENTER);
        return p;
    }

    /** 0 = neutre, 1 = conforme, 2 = erreurs. */
    private void setBanner(int kind) {
        switch (kind) {
            case 1 -> {
                banner.setFill(AppTheme.SUCCESS_LIGHT); banner.setLine(AppTheme.mix(AppTheme.SUCCESS, Color.WHITE, .7));
                bannerIcon.setIcon(Icons.chip(Icons.CHECK_CIRCLE, 40, 22, AppTheme.SUCCESS, Color.WHITE, true));
            }
            case 2 -> {
                banner.setFill(AppTheme.DANGER_LIGHT); banner.setLine(AppTheme.mix(AppTheme.DANGER, Color.WHITE, .7));
                bannerIcon.setIcon(Icons.chip(Icons.ALERT_CIRCLE, 40, 22, AppTheme.DANGER, Color.WHITE, true));
            }
            default -> {
                banner.setFill(AppTheme.BG_CARD); banner.setLine(AppTheme.BORDER);
                bannerIcon.setIcon(Icons.chip(Icons.SHIELD_CHECK, 40, 22, AppTheme.CRIMSON, AppTheme.PRIMARY_LIGHT, true));
            }
        }
    }

    //  Corps 
    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout(0, AppTheme.GAP));
        p.setOpaque(false);

        // Règles actives
        JPanel regles = SwingUtil.card(new GridLayout(2, 2, AppTheme.GAP, AppTheme.GAP));
        String[] noms = {
            "Chevauchement de salle",
            "Professeur sur deux créneaux",
            "Temps de repos (1h min)",
            "Conformité composition jury"
        };
        for (String n : noms) {
            JLabel l = AppTheme.body(n);
            l.setIcon(Icons.icon(Icons.SHIELD_CHECK, 18, AppTheme.ROSE_DEEP));
            l.setIconTextGap(10);
            l.setBorder(new EmptyBorder(4,4,4,8));
            regles.add(l);
        }
        JPanel topCard = new JPanel(new BorderLayout(0, AppTheme.GAP));
        topCard.setOpaque(false);
        topCard.add(SwingUtil.sectionBar("Règles actives (4 validators)"), BorderLayout.NORTH);
        topCard.add(regles, BorderLayout.CENTER);
        p.add(topCard, BorderLayout.NORTH);

        // Table des erreurs
        JTable errTable = new JTable(errModel);
        SwingUtil.styleTable(errTable);
        errTable.getColumnModel().getColumn(0).setPreferredWidth(190);
        errTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        JPanel errCard = new JPanel(new BorderLayout(0, AppTheme.GAP));
        errCard.setOpaque(false);
        errCard.add(SwingUtil.sectionBar("Résultats de la vérification"), BorderLayout.NORTH);
        SwingUtil.pillColumn(errTable, 0,
            name -> new Color[]{AppTheme.BURGUNDY, AppTheme.PRIMARY_LIGHT}, false);
        errScroll = SwingUtil.scrollPane(errTable, "Lancez la vérification pour afficher les résultats");
        errCard.add(errScroll, BorderLayout.CENTER);
        p.add(errCard, BorderLayout.CENTER);

        return p;
    }

    // Vérification
    private void lancerVerification() {
        if (planning == null) return;
        errModel.setRowCount(0);

        ValidationService.ValidationResult res = ctx.getValidationService().valider(planning);

        if (res.isValide()) {
            lblStatut.setText("Planning conforme — aucune erreur détectée !");
            lblStatut.setForeground(AppTheme.SUCCESS);
            lblDetails.setText(planning.getSoutenances().size() + " soutenances vérifiées.");
            setBanner(1);
            errScroll.setEmptyText("Aucune erreur détectée");
        } else {
            lblStatut.setText(res.getNbErreurs() + " erreur(s) détectée(s)");
            lblStatut.setForeground(AppTheme.DANGER);
            lblDetails.setText("Corrigez les erreurs ci-dessous avant d'exporter.");
            setBanner(2);
            for (ValidationService.ErreurValidator e : res.getErreurs())
                errModel.addRow(new Object[]{e.getValidator(), e.getMessage()});
        }
    }
}

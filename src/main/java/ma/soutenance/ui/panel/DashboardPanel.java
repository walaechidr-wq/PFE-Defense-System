package ma.soutenance.ui.panel;

import ma.soutenance.config.AppContext;
import ma.soutenance.model.*;
import ma.soutenance.model.enums.Specialite;
import ma.soutenance.ui.MainFrame;
import ma.soutenance.ui.component.ResponsiveGrid;
import ma.soutenance.ui.component.RoundedPanel;
import ma.soutenance.ui.util.AppTheme;
import ma.soutenance.ui.util.Icons;
import ma.soutenance.ui.util.SwingUtil;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tableau de bord — v2.0 : compatible FiliereDynamique.
 * filiereToSpec() utilise getSpecialiteEncadrement() au lieu du switch sur l'enum.
 */
public class DashboardPanel extends JPanel {

    private final AppContext ctx = AppContext.get();

    public DashboardPanel() {
        setLayout(new BorderLayout(0, AppTheme.GAP));
        setBackground(AppTheme.BG_APP);
        setBorder(new EmptyBorder(AppTheme.PAD, AppTheme.PAD, AppTheme.PAD, AppTheme.PAD));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    /** Appelé par MainFrame après un import Excel réussi. */
    public void refresh() {
        removeAll();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel buildHeader() {
        Configuration cfg = ctx.getConfiguration();
        String info = cfg != null
            ? "Période : " + cfg.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
              + "  •  " + cfg.getNombreJours() + " jours  •  "
              + ctx.getSalles().size() + " salles  •  Capacité : "
              + cfg.getCapaciteTotale(ctx.getSalles().size()) + " créneaux"
            : "Vue d'ensemble de la session de soutenances";
        return SwingUtil.pageHeader("Tableau de bord", AppTheme.muted(info), Icons.DASHBOARD, null);
    }

    private JComponent buildBody() {
        // Avant l'import : état vide avec appel à l'action
        if (!ctx.isInitialise()) {
            JButton imp = AppTheme.btnPrimary("Importer un fichier Excel", Icons.UPLOAD);
            imp.addActionListener(e -> {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w instanceof MainFrame mf) mf.importerExcel();
            });
            return SwingUtil.emptyState(Icons.UPLOAD, "Aucune donnée importée",
                "Importez votre fichier Excel PFE pour afficher les statistiques, "
                + "puis générer l'affectation des encadrants et le planning.", imp);
        }
        JPanel p = new JPanel(new BorderLayout(AppTheme.GAP, AppTheme.GAP));
        p.setOpaque(false);
        p.add(buildKPIs(),   BorderLayout.NORTH);
        p.add(buildCharts(), BorderLayout.CENTER);
        return SwingUtil.verticalScroll(p);
    }

    private JPanel buildKPIs() {
        JPanel p = new JPanel(new ResponsiveGrid(170, AppTheme.GAP, 120, false));
        p.setOpaque(false);

        long nbAnglais = ctx.getGroupes().stream().filter(Groupe::isAnglais).count();
        long nbBinomes = ctx.getGroupes().stream().filter(Groupe::isBinome).count();
        p.add(AppTheme.kpiHero(String.valueOf(ctx.getGroupes().size()), "Groupes", Icons.LAYERS));
        p.add(AppTheme.kpiCard(String.valueOf(ctx.getEtudiants().size()),   "Étudiants",   AppTheme.CRIMSON,   Icons.CAP));
        p.add(AppTheme.kpiCard(String.valueOf(ctx.getProfesseurs().size()), "Professeurs", AppTheme.ROSE_DEEP, Icons.USER_CHECK));
        p.add(AppTheme.kpiCard(String.valueOf(nbBinomes),                   "Binômes",     AppTheme.BURGUNDY,  Icons.LINK));
        p.add(AppTheme.kpiCard(String.valueOf(nbAnglais),                   "En Anglais",
                               AppTheme.mix(AppTheme.BURGUNDY, AppTheme.ROSE, .55), Icons.GLOBE));
        return p;
    }

    private JPanel buildCharts() {
        JPanel p = new JPanel(new ResponsiveGrid(320, AppTheme.GAP, 300, true));
        p.setOpaque(false);
        p.add(chartCard("Groupes par filière",     pieFiliere()));
        p.add(chartCard("Profs par spécialité",    pieSpecialite()));
        p.add(chartCard("Charge estimée / prof",   barCharge()));
        return p;
    }

    /** Carte blanche : intitulé de section + graphique. */
    private JPanel chartCard(String title, ChartPanel cp) {
        RoundedPanel c = new RoundedPanel(new BorderLayout(0, 6), AppTheme.R_MD, AppTheme.BG_CARD, AppTheme.BORDER, true);
        c.setBorder(new EmptyBorder(14, 14, 16, 16));
        c.add(SwingUtil.sectionBar(title), BorderLayout.NORTH);
        c.add(cp, BorderLayout.CENTER);
        return c;
    }

    private void styleChartPanel(ChartPanel cp) {
        cp.setBorder(BorderFactory.createEmptyBorder());
        cp.setBackground(AppTheme.BG_CARD);
        // pas de mise à l'échelle des polices : le graphique se redessine à la taille réelle
        cp.setMinimumDrawWidth(120);  cp.setMinimumDrawHeight(120);
        cp.setMaximumDrawWidth(4000); cp.setMaximumDrawHeight(4000);
        cp.setPopupMenu(null);
    }

    private ChartPanel pieFiliere() {
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        ctx.getGroupes().stream()
            .filter(g -> g.getFiliere() != null)
            .collect(Collectors.groupingBy(
                g -> g.getFiliere().getLibelle(), Collectors.counting()))
            .forEach(ds::setValue);
        return pie(ds);
    }

    private ChartPanel pieSpecialite() {
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        ctx.getProfesseurs().stream()
            .collect(Collectors.groupingBy(
                p -> p.getSpecialite().getLibelle(), Collectors.counting()))
            .forEach(ds::setValue);
        return pie(ds);
    }

    private ChartPanel pie(DefaultPieDataset<String> ds) {
        JFreeChart chart = ChartFactory.createPieChart(null, ds, true, true, false);
        stylePie(chart, ds);
        ChartPanel cp = new ChartPanel(chart);
        styleChartPanel(cp);
        return cp;
    }

    private ChartPanel barCharge() {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();

        Map<Specialite, Long> profsParSpec = ctx.getProfesseurs().stream()
            .collect(Collectors.groupingBy(Professeur::getSpecialite, Collectors.counting()));
        Map<Specialite, Long> grpParSpec = ctx.getGroupes().stream()
            .filter(g -> g.getFiliere() != null)
            .collect(Collectors.groupingBy(
                g -> filiereToSpec(g.getFiliere()), Collectors.counting()));

        ctx.getProfesseurs().stream().limit(12).forEach(p -> {
            long nb  = grpParSpec.getOrDefault(p.getSpecialite(), 0L);
            long nps = profsParSpec.getOrDefault(p.getSpecialite(), 1L);
            ds.addValue((double) nb / nps, "Charge estimée", p.getNom());
        });

        JFreeChart chart = ChartFactory.createBarChart(
            null, null, "Groupes", ds,
            PlotOrientation.VERTICAL, false, true, false);
        styleBar(chart);
        ChartPanel cp = new ChartPanel(chart);
        styleChartPanel(cp);
        return cp;
    }

    /** Camembert aux couleurs de la marque ; légende « libellé · valeur (pourcentage) ». */
    @SuppressWarnings("unchecked")
    private void stylePie(JFreeChart chart, DefaultPieDataset<String> ds) {
        chart.setBackgroundPaint(AppTheme.BG_CARD);
        chart.setBorderVisible(false);
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(AppTheme.BG_CARD);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setSectionOutlinesVisible(true);               // liseré blanc entre les parts
        plot.setDefaultSectionOutlinePaint(Color.WHITE);
        plot.setDefaultSectionOutlineStroke(new BasicStroke(2f));
        plot.setLabelGenerator(null);                       // étiquettes sur le disque retirées : tout est dans la légende
        plot.setInteriorGap(0.04);
        plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0} · {1} ({2})"));
        int i = 0;
        for (String k : ds.getKeys())
            plot.setSectionPaint(k, AppTheme.CHART_COLORS[i++ % AppTheme.CHART_COLORS.length]);

        LegendTitle lg = chart.getLegend();
        if (lg != null) {
            lg.setItemFont(AppTheme.F_SMALL);
            lg.setItemPaint(AppTheme.TEXT_SECONDARY);
            lg.setBackgroundPaint(AppTheme.BG_CARD);
            lg.setFrame(BlockBorder.NONE);
        }
    }

    /** Histogramme à barres crimson, quadrillage léger, axes épurés. */
    private void styleBar(JFreeChart chart) {
        chart.setBackgroundPaint(AppTheme.BG_CARD);
        chart.setBorderVisible(false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(AppTheme.BG_CARD);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(AppTheme.BORDER);
        plot.setRangeGridlineStroke(new BasicStroke(1f));

        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setBarPainter(new StandardBarPainter());
        r.setShadowVisible(false);
        r.setDrawBarOutline(false);
        r.setSeriesPaint(0, AppTheme.CRIMSON);
        r.setMaximumBarWidth(0.08);

        CategoryAxis dom = plot.getDomainAxis();
        dom.setTickLabelFont(AppTheme.F_SMALL);
        dom.setTickLabelPaint(AppTheme.TEXT_SECONDARY);
        dom.setAxisLinePaint(AppTheme.BORDER_STRONG);
        dom.setTickMarksVisible(false);
        dom.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        ValueAxis rng = plot.getRangeAxis();
        rng.setTickLabelFont(AppTheme.F_SMALL);
        rng.setTickLabelPaint(AppTheme.TEXT_SECONDARY);
        rng.setLabelFont(AppTheme.F_SMALL);
        rng.setLabelPaint(AppTheme.TEXT_SECONDARY);
        rng.setAxisLineVisible(false);
        rng.setTickMarksVisible(false);
    }

    /**
     * v2.0 — utilise Filiere.getSpecialiteEncadrement()
     */
    private Specialite filiereToSpec(Filiere f) {
        if (f == null) return Specialite.INFORMATIQUE;
        return Specialite.fromString(f.getSpecialiteEncadrement());
    }
}
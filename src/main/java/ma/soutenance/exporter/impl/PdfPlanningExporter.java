package ma.soutenance.exporter.impl;
import java.util.List;   

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import ma.soutenance.exporter.PlanningExporter;
import ma.soutenance.model.*;
import ma.soutenance.ui.util.ProfesseurColorRegistry;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Export PDF du planning — OpenPDF.
 *
 * v2.0 — Code couleur par professeur :
 *   La cellule "Encadrant" dans le tableau PDF est colorée avec la couleur
 *   unique du professeur (PdfPCell.setBackgroundColor()).
 *   Une page de légende est ajoutée en fin de document.
 */
public class PdfPlanningExporter implements PlanningExporter {

    private static final Color C_PRIMARY = new Color(37, 99, 235);
    private static final Color C_HEADER  = new Color(30, 64, 175);
    private static final Color C_ALT     = new Color(248, 250, 252);
    private static final Color C_ANGLAIS = new Color(254, 243, 199);
    private static final Color C_TEXT    = new Color(15, 23, 42);

    private final ProfesseurColorRegistry colorRegistry = ProfesseurColorRegistry.get();

    @Override
    public void export(Planning planning, String outputPath) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 36, 28);
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();
        addTitle(doc, planning);
        addPlanningTable(doc, planning);
        addStatPage(doc, planning);
        addLegendePage(doc, planning);
        doc.close();
    }

    // ── Titre ─────────────────────────────────────────────────────────────────
    private void addTitle(Document doc, Planning planning) throws DocumentException {
        Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, C_PRIMARY);
        Font fSub   = FontFactory.getFont(FontFactory.HELVETICA, 12, C_TEXT);
        Font fInfo  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

        Paragraph t = new Paragraph("PLANNING DES SOUTENANCES PFE", fTitle);
        t.setAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(10);
        doc.add(t);

        Paragraph sub = new Paragraph("École Nationale des Sciences Appliquées — UAE", fSub);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        doc.add(sub);

        Configuration cfg = planning.getConfiguration();
        String periode = cfg != null
            ? cfg.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
              + " → " + cfg.getDateDebut().plusDays(cfg.getNombreJours() - 1)
                           .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "N/A";

        Paragraph info = new Paragraph(
            "Période : " + periode + "   |   Généré le : "
            + planning.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            + "   |   " + planning.getSoutenances().size() + " soutenances", fInfo);
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingBefore(4);
        info.setSpacingAfter(14);
        doc.add(info);

        doc.add(new Chunk(new LineSeparator(1.5f, 100, C_PRIMARY, Element.ALIGN_CENTER, 0)));
        doc.add(Chunk.NEWLINE);
    }

    // ── Tableau principal avec couleurs encadrant ─────────────────────────────
    private void addPlanningTable(Document doc, Planning planning)
            throws DocumentException {

        // ← Déclarer ici, pas en champs statiques
        Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fCell = FontFactory.getFont(FontFactory.HELVETICA,      8, C_TEXT);
        Font fDate = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, C_PRIMARY);
        Font fEnc  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_TEXT);

       
        Map<LocalDate, List<Soutenance>> byDate = new TreeMap<>(planning.getSoutenancesByDate());
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);

        for (Map.Entry<LocalDate, List<Soutenance>> entry : byDate.entrySet()) {
            Paragraph dayTitle = new Paragraph(entry.getKey().format(dayFmt).toUpperCase(), fDate);
            dayTitle.setSpacingBefore(14);
            dayTitle.setSpacingAfter(5);
            doc.add(dayTitle);

            PdfPTable tbl = new PdfPTable(new float[]{1.2f, 2.8f, .9f, 2.2f, 2.8f, 1f, .9f});
            tbl.setWidthPercentage(100);

            for (String h : new String[]{"Heure","Groupe(s)","Filière","Encadrant","Membres Jury","Salle","Langue"})
                tbl.addCell(headerCell(h, fHead));

            boolean alt = false;
            for (Soutenance s : entry.getValue()) {
                // Couleur de base pour les cellules standards
                Color bgBase = s.isAnglais() ? C_ANGLAIS : (alt ? C_ALT : Color.WHITE);

                // Couleur unique de l'encadrant pour sa cellule
                Color bgEnc = (s.getEncadrant() != null)
                    ? colorRegistry.getCouleur(s.getEncadrant())
                    : bgBase;

                alt = !alt;

                tbl.addCell(dataCell(
                    s.getCreneau() != null
                        ? s.getCreneau().getHeureDebut() + "-" + s.getCreneau().getHeureFin() : "?",
                    fCell, bgBase, Element.ALIGN_CENTER));

                tbl.addCell(dataCell(
                    s.getGroupe() != null ? s.getGroupe().getLabel() : "?",
                    fCell, bgBase, Element.ALIGN_LEFT));

                tbl.addCell(dataCell(
                    s.getGroupe() != null ? s.getGroupe().getFiliere().name() : "?",
                    fCell, bgBase, Element.ALIGN_CENTER));

                // ← Cellule Encadrant avec sa couleur unique
                tbl.addCell(dataCell(
                    s.getEncadrant() != null ? s.getEncadrant().getFullName() : "?",
                    fEnc, bgEnc, Element.ALIGN_LEFT));

                String jury = s.getAutresJury().stream()
                    .map(Professeur::getFullName)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
                tbl.addCell(dataCell(jury, fCell, bgBase, Element.ALIGN_LEFT));
                tbl.addCell(dataCell(s.getSalleNom(), fCell, bgBase, Element.ALIGN_CENTER));
                tbl.addCell(dataCell(s.isAnglais() ? "EN" : "FR", fCell, bgBase, Element.ALIGN_CENTER));
            }
            doc.add(tbl);
        }
    }

    // ── Page statistiques ─────────────────────────────────────────────────────
    private void addStatPage(Document doc, Planning planning) throws DocumentException {
        doc.newPage();
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, C_PRIMARY);
        Font fLab     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_TEXT);
        Font fVal     = FontFactory.getFont(FontFactory.HELVETICA, 9, C_TEXT);

        Paragraph stat = new Paragraph("STATISTIQUES", fSection);
        stat.setSpacingAfter(10);
        doc.add(stat);

        Paragraph fil = new Paragraph("Répartition par filière", fLab);
        fil.setSpacingAfter(4);
        doc.add(fil);

        PdfPTable t1 = new PdfPTable(2);
        t1.setWidthPercentage(45);
        planning.getStatsByFiliere().forEach((k, v) -> {
            t1.addCell(new Phrase(k, fVal));
            t1.addCell(new Phrase(v + " groupe(s)", fVal));
        });
        doc.add(t1);
        doc.add(Chunk.NEWLINE);

        Paragraph charge = new Paragraph("Charge des encadrants", fLab);
        charge.setSpacingAfter(4);
        doc.add(charge);

        PdfPTable t2 = new PdfPTable(2);
        t2.setWidthPercentage(55);
        planning.getChargeByEncadrant().entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> {
                t2.addCell(new Phrase(e.getKey(), fVal));
                t2.addCell(new Phrase(e.getValue() + " groupe(s)", fVal));
            });
        doc.add(t2);
        doc.add(Chunk.NEWLINE);

        Font fInfo = FontFactory.getFont(FontFactory.HELVETICA, 10, C_TEXT);
        doc.add(new Paragraph("Soutenances en anglais : " + planning.getNbAnglais(), fInfo));
        doc.add(new Paragraph("Soutenances en français : "
            + (planning.getSoutenances().size() - planning.getNbAnglais()), fInfo));
    }

    // ── Page légende des couleurs ──────────────────────────────────────────────
    private void addLegendePage(Document doc, Planning planning) throws DocumentException {
        doc.newPage();
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, C_PRIMARY);
        Font fLab     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fVal     = FontFactory.getFont(FontFactory.HELVETICA, 9, C_TEXT);
        Font fBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_TEXT);

        Paragraph leg = new Paragraph("LÉGENDE — CODE COULEUR DES ENCADRANTS", fSection);
        leg.setSpacingAfter(12);
        doc.add(leg);

        PdfPTable tbl = new PdfPTable(new float[]{3f, 2f, 1.5f});
        tbl.setWidthPercentage(70);

        // En-têtes
        for (String h : new String[]{"Professeur", "Spécialité", "Couleur"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h, fLab));
            hc.setBackgroundColor(C_HEADER);
            hc.setPadding(5);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(hc);
        }

        // Une ligne par encadrant unique du planning
        planning.getSoutenances().stream()
            .map(Soutenance::getEncadrant)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparing(Professeur::getFullName))
            .forEach(prof -> {
                Color bg = colorRegistry.getCouleur(prof);

                PdfPCell cName = new PdfPCell(new Phrase(prof.getFullName(), fBold));
                cName.setBackgroundColor(bg);
                cName.setPadding(5);
                tbl.addCell(cName);

                PdfPCell cSpec = new PdfPCell(new Phrase(
                    prof.getSpecialite() != null ? prof.getSpecialite().getLibelle() : "?", fVal));
                cSpec.setBackgroundColor(bg);
                cSpec.setPadding(5);
                tbl.addCell(cSpec);

                PdfPCell cColor = new PdfPCell(new Phrase(colorRegistry.getCouleurHex(prof), fVal));
                cColor.setBackgroundColor(bg);
                cColor.setPadding(5);
                cColor.setHorizontalAlignment(Element.ALIGN_CENTER);
                tbl.addCell(cColor);
            });

        doc.add(tbl);
    }

    // ── Helpers cellules ──────────────────────────────────────────────────────
    private PdfPCell headerCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(C_HEADER);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell dataCell(String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    @Override public String getFormat() { return "PDF"; }
}
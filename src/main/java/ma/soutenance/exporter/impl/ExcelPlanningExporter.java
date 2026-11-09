package ma.soutenance.exporter.impl;

import ma.soutenance.exporter.PlanningExporter;
import ma.soutenance.model.*;
import ma.soutenance.ui.util.ProfesseurColorRegistry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Export Excel du planning — Apache POI.
 *
 * v2.0 — Code couleur par professeur :
 *   La colonne "Encadrant" est colorée avec la couleur unique du professeur
 *   (issue de ProfesseurColorRegistry). Même palette que l'UI et le PDF.
 */
public class ExcelPlanningExporter implements PlanningExporter {

    private final ProfesseurColorRegistry colorRegistry = ProfesseurColorRegistry.get();

    @Override
    public void export(Planning planning, String outputPath) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Cache de styles couleur : on en crée un par couleur unique (limite POI = 64000)
            Map<String, CellStyle> styleCache = new HashMap<>();

            feuillePlanning(wb, planning, styleCache);
            feuilleAffectations(wb, planning, styleCache);
            feuilleStats(wb, planning);
            feuilleLegendeCouleurs(wb, planning);

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
    }

    // ── Feuille 1 : Planning journalier ──────────────────────────────────────
    private void feuillePlanning(XSSFWorkbook wb, Planning planning,
                                  Map<String, CellStyle> styleCache) {
        XSSFSheet sheet = wb.createSheet("Planning");
        CellStyle csTitle = csTitle(wb);
        CellStyle csHead  = csHead(wb);

        int rn = 0;

        // Titre
        XSSFRow title = sheet.createRow(rn++);
        title.setHeight((short) 900);
        XSSFCell tc = title.createCell(0);
        tc.setCellValue("PLANNING DES SOUTENANCES PFE — "
            + (planning.getConfiguration() != null
                ? planning.getConfiguration().getDateDebut().getYear() : ""));
        tc.setCellStyle(csTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        rn++;

        // En-têtes
        String[] heads = {"Date","Heure","Groupe(s)","Filière","Encadrant","Membres Jury","Salle","Langue"};
        XSSFRow hr = sheet.createRow(rn++);
        hr.setHeight((short) 550);
        for (int i = 0; i < heads.length; i++) {
            XSSFCell c = hr.createCell(i);
            c.setCellValue(heads[i]);
            c.setCellStyle(csHead);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);
        Map<LocalDate, List<Soutenance>> byDate = new TreeMap<>(planning.getSoutenancesByDate());

        for (Map.Entry<LocalDate, List<Soutenance>> e : byDate.entrySet()) {
            for (Soutenance s : e.getValue()) {
                XSSFRow row = sheet.createRow(rn++);
                row.setHeight((short) 420);

                // Style de base (anglais ou standard)
                CellStyle csBase = s.isAnglais() ? csAnglais(wb) : csData(wb);

                // Style coloré pour la colonne Encadrant
                CellStyle csEnc = getCouleurStyle(wb, s.getEncadrant(), styleCache);

                set(row, 0, e.getKey().format(df), csBase);
                set(row, 1, s.getCreneau() != null
                    ? s.getCreneau().getHeureDebut() + "-" + s.getCreneau().getHeureFin() : "?", csBase);
                set(row, 2, s.getGroupe() != null ? s.getGroupe().getLabel() : "?", csBase);
                set(row, 3, s.getGroupe() != null ? s.getGroupe().getFiliere().name() : "?", csBase);

                // ← Colonne Encadrant colorée
                set(row, 4, s.getEncadrant() != null ? s.getEncadrant().getFullName() : "?", csEnc);

                String jury = s.getAutresJury().stream()
                    .map(Professeur::getFullName)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + " | " + b);
                set(row, 5, jury, csBase);
                set(row, 6, s.getSalleNom(), csBase);
                set(row, 7, s.isAnglais() ? "Anglais" : "Français", csBase);
            }
        }

        int[] w = {12, 12, 38, 10, 24, 38, 10, 10};
        for (int i = 0; i < w.length; i++) sheet.setColumnWidth(i, w[i] * 256);
    }

    // ── Feuille 2 : Affectations Encadrants ──────────────────────────────────
    private void feuilleAffectations(XSSFWorkbook wb, Planning planning,
                                      Map<String, CellStyle> styleCache) {
        XSSFSheet sheet = wb.createSheet("Affectations Encadrants");
        CellStyle csHead = csHead(wb);

        int rn = 0;
        XSSFRow title = sheet.createRow(rn++);
        title.setHeight((short) 900);
        XSSFCell tc = title.createCell(0);
        tc.setCellValue("AFFECTATIONS DES ENCADRANTS — PFE");
        tc.setCellStyle(csTitle(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rn++;

        String[] heads = {"Groupe(s)","Filière","Langue","Encadrant","Spécialité","Membres Jury"};
        XSSFRow hr = sheet.createRow(rn++);
        hr.setHeight((short) 550);
        for (int i = 0; i < heads.length; i++) {
            XSSFCell c = hr.createCell(i);
            c.setCellValue(heads[i]);
            c.setCellStyle(csHead);
        }

        for (Soutenance s : planning.getSoutenances()) {
            XSSFRow row = sheet.createRow(rn++);
            row.setHeight((short) 420);

            CellStyle csBase = csData(wb);
            CellStyle csEnc  = getCouleurStyle(wb, s.getEncadrant(), styleCache);

            set(row, 0, s.getGroupe() != null ? s.getGroupe().getLabel() : "?", csBase);
            set(row, 1, s.getGroupe() != null ? s.getGroupe().getFiliere().name() : "?", csBase);
            set(row, 2, s.isAnglais() ? "Anglais" : "Français", csBase);
            // ← Colonne Encadrant colorée
            set(row, 3, s.getEncadrant() != null ? s.getEncadrant().getFullName() : "?", csEnc);
            set(row, 4, s.getEncadrant() != null ? s.getEncadrant().getSpecialite().getLibelle() : "?", csBase);
            String jury = s.getAutresJury().stream()
                .map(Professeur::getFullName)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " | " + b);
            set(row, 5, jury, csBase);
        }

        int[] w = {38, 10, 10, 26, 18, 38};
        for (int i = 0; i < w.length; i++) sheet.setColumnWidth(i, w[i] * 256);
    }

    // ── Feuille 3 : Statistiques ──────────────────────────────────────────────
    private void feuilleStats(XSSFWorkbook wb, Planning planning) {
        XSSFSheet sheet = wb.createSheet("Statistiques");
        CellStyle csHead = csHead(wb);
        CellStyle csData = csData(wb);
        CellStyle csAlt  = csAlt(wb);

        int rn = 0;
        XSSFRow h1 = sheet.createRow(rn++);
        set(h1, 0, "FILIÈRE", csHead);
        set(h1, 1, "NB GROUPES", csHead);

        boolean alt = false;
        for (Map.Entry<String, Long> e : planning.getStatsByFiliere().entrySet()) {
            XSSFRow r = sheet.createRow(rn++);
            CellStyle st = alt ? csAlt : csData;
            set(r, 0, e.getKey(), st);
            XSSFCell nc = r.createCell(1);
            nc.setCellValue(e.getValue());
            nc.setCellStyle(st);
            alt = !alt;
        }
        rn++;

        XSSFRow h2 = sheet.createRow(rn++);
        set(h2, 0, "ENCADRANT", csHead);
        set(h2, 1, "NB GROUPES", csHead);

        alt = false;
        for (Map.Entry<String, Long> e : new TreeMap<>(planning.getChargeByEncadrant()).entrySet()) {
            XSSFRow r = sheet.createRow(rn++);
            CellStyle st = alt ? csAlt : csData;
            set(r, 0, e.getKey(), st);
            XSSFCell nc = r.createCell(1);
            nc.setCellValue(e.getValue());
            nc.setCellStyle(st);
            alt = !alt;
        }
        rn++;

        XSSFRow tot = sheet.createRow(rn++);
        set(tot, 0, "Total soutenances", csHead);
        XSSFCell tc = tot.createCell(1);
        tc.setCellValue(planning.getSoutenances().size());
        tc.setCellStyle(csHead);

        XSSFRow en = sheet.createRow(rn++);
        set(en, 0, "Dont Anglais", csData);
        XSSFCell ec = en.createCell(1);
        ec.setCellValue(planning.getNbAnglais());
        ec.setCellStyle(csData);

        sheet.setColumnWidth(0, 34 * 256);
        sheet.setColumnWidth(1, 14 * 256);
    }

    // ── Feuille 4 : Légende des couleurs ──────────────────────────────────────
    private void feuilleLegendeCouleurs(XSSFWorkbook wb, Planning planning) {
        XSSFSheet sheet = wb.createSheet("Légende Couleurs");
        CellStyle csHead = csHead(wb);

        int rn = 0;
        XSSFRow hr = sheet.createRow(rn++);
        set(hr, 0, "Professeur", csHead);
        set(hr, 1, "Spécialité", csHead);
        set(hr, 2, "Couleur", csHead);

        // ← Remplacer le forEach stream par une boucle for classique
        List<Professeur> profsUniques = planning.getSoutenances().stream()
            .map(Soutenance::getEncadrant)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparing(Professeur::getFullName))
            .collect(java.util.stream.Collectors.toList());

        for (Professeur prof : profsUniques) {
            XSSFRow row = sheet.createRow(rn++);
            Color c = colorRegistry.getCouleur(prof);

            XSSFCellStyle csColor = wb.createCellStyle();
            csColor.setFillForegroundColor(
                new XSSFColor(ProfesseurColorRegistry.toXssfBytes(c), null));
            csColor.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            csColor.setBorderBottom(BorderStyle.THIN);

            set(row, 0, prof.getFullName(), csColor);
            set(row, 1, prof.getSpecialite() != null
                ? prof.getSpecialite().getLibelle() : "?", csColor);
            set(row, 2, colorRegistry.getCouleurHex(prof), csColor);
        }

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 12 * 256);
    }

    // ── Style couleur encadrant ───────────────────────────────────────────────

    /**
     * Retourne (ou crée et met en cache) un CellStyle avec la couleur de fond
     * correspondant au professeur. Le cache évite de dépasser la limite POI.
     */
    private CellStyle getCouleurStyle(XSSFWorkbook wb, Professeur prof,
                                       Map<String, CellStyle> cache) {
        if (prof == null) return csData(wb);

        String key = prof.getId();
        return cache.computeIfAbsent(key, k -> {
            XSSFCellStyle s = wb.createCellStyle();
            Color c = colorRegistry.getCouleur(prof);
            s.setFillForegroundColor(
                new XSSFColor(ProfesseurColorRegistry.toXssfBytes(c), null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setBorderBottom(BorderStyle.HAIR);
            s.setWrapText(true);
            s.setVerticalAlignment(VerticalAlignment.CENTER);

            // Texte en gras pour distinguer l'encadrant
            XSSFFont font = wb.createFont();
            font.setBold(true);
            s.setFont(font);
            return s;
        });
    }

    // ── Styles de base ────────────────────────────────────────────────────────
    private CellStyle csTitle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 15);
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)37,(byte)99,(byte)235}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle csHead(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)30,(byte)64,(byte)175}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setWrapText(true);
        return s;
    }

    private CellStyle csData(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.HAIR);
        s.setWrapText(true);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle csAlt(XSSFWorkbook wb) {
        CellStyle s = csData(wb);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)241,(byte)245,(byte)249}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle csAnglais(XSSFWorkbook wb) {
        CellStyle s = csData(wb);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)254,(byte)243,(byte)199}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void set(XSSFRow row, int col, String val, CellStyle st) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(st);
    }

    @Override public String getFormat() { return "XLSX"; }
}
package ma.soutenance.repository.impl;

import ma.soutenance.exception.ExcelParseException;
import ma.soutenance.model.*;
import ma.soutenance.model.enums.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Charge toutes les données depuis le fichier Excel.
 * v2.1 — Ouverture unique du Workbook pour toutes les feuilles (5x plus rapide).
 */
public class ExcelDataLoader {

    private String resourcePath;

    public ExcelDataLoader(String resourcePath) { this.resourcePath = resourcePath; }

    public void setResourcePath(String path) { this.resourcePath = path; }
    public String getResourcePath()          { return resourcePath; }

    // ── Ouverture unique ──────────────────────────────────────────────────────
    private Workbook openWorkbook() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is != null) return new XSSFWorkbook(is);
        return new XSSFWorkbook(new FileInputStream(resourcePath));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE — charge tout en une seule ouverture du fichier
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Charge toutes les données en ouvrant le fichier Excel UNE SEULE FOIS.
     * Retourne un objet contenant toutes les listes.
     */
    public DonneesExcel loadAll() {
        try (Workbook wb = openWorkbook()) {
            // 1. Filières en premier (FiliereRegistry doit être initialisé avant les autres)
            List<Filiere> filieres = loadFilieresDynamiquesDepuis(wb);
            FiliereRegistry.init(filieres);

            // 2. Étudiants (utilise FiliereRegistry)
            List<Etudiant> etudiants = loadEtudiantsDepuis(wb);

            // 3. Reste des données
            List<Professeur>  professeurs   = loadProfesseursDepuis(wb);
            List<Salle>       salles        = loadSallesDepuis(wb);
            Configuration     configuration = loadConfigurationDepuis(wb);
            List<Groupe>      groupes       = loadGroupesDepuis(wb, etudiants);

            return new DonneesExcel(filieres, etudiants, professeurs,
                                    salles, configuration, groupes);

        } catch (ExcelParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExcelParseException("Excel", -1, ex.getMessage(), ex);
        }
    }

    /**
     * Conteneur immuable des données chargées depuis Excel.
     */
    public static class DonneesExcel {
        public final List<Filiere>    filieres;
        public final List<Etudiant>   etudiants;
        public final List<Professeur> professeurs;
        public final List<Salle>      salles;
        public final Configuration    configuration;
        public final List<Groupe>     groupes;

        public DonneesExcel(List<Filiere> filieres, List<Etudiant> etudiants,
                             List<Professeur> professeurs, List<Salle> salles,
                             Configuration configuration, List<Groupe> groupes) {
            this.filieres      = filieres;
            this.etudiants     = etudiants;
            this.professeurs   = professeurs;
            this.salles        = salles;
            this.configuration = configuration;
            this.groupes       = groupes;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODES INTERNES — reçoivent le Workbook déjà ouvert
    // ════════════════════════════════════════════════════════════════════════

    private List<Filiere> loadFilieresDynamiquesDepuis(Workbook wb) {
        try {
            Sheet sheet = wb.getSheet("Filieres");
            if (sheet == null) {
                // Cherche avec normalisation des accents
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    if (normaliser(wb.getSheetName(i)).equals("filieres"))
                        sheet = wb.getSheetAt(i);
                }
            }
            if (sheet == null) return filieresParDefaut();

            List<Filiere> list = new ArrayList<>();
            int startRow = isHeaderRow(sheet.getRow(0)) ? 1 : 0;

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row)) continue;

                String col0 = cell(row, 0);
                String col1 = cell(row, 1);
                String col2 = cell(row, 2);

                String code, libelle, spec;
                if (!col0.isEmpty() && col1.isEmpty()) {
                    libelle = col0;
                    code    = genererCode(libelle);
                    spec    = determinerSpecialite(libelle);
                } else {
                    code    = col0;
                    libelle = col1;
                    spec    = col2.isBlank() ? determinerSpecialite(libelle) : col2;
                }
                if (!code.isBlank() && !libelle.isBlank())
                    list.add(new Filiere(code, libelle, spec));
            }
            return list.isEmpty() ? filieresParDefaut() : list;

        } catch (Exception e) {
            return filieresParDefaut();
        }
    }

    private List<Etudiant> loadEtudiantsDepuis(Workbook wb) {
        List<Etudiant> list = new ArrayList<>();
        Sheet sheet = requireSheet(wb, "Etudiants");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row)) continue;
            try {
                Filiere f = FiliereRegistry.fromString(cell(row, 5));
                list.add(new Etudiant(
                    cell(row,0), cell(row,1), cell(row,2),
                    cell(row,3), cell(row,4), f));
            } catch (Exception e) {
                throw new ExcelParseException("Etudiants", i, e.getMessage(), e);
            }
        }
        return list;
    }

    private List<Professeur> loadProfesseursDepuis(Workbook wb) {
        List<Professeur> list = new ArrayList<>();
        Sheet sheet = requireSheet(wb, "Professeurs");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row)) continue;
            try {
                String nom    = cell(row, 0);
                String prenom = cell(row, 1);
                Specialite sp = Specialite.fromString(cell(row, 2));
                String id     = (nom + "_" + prenom).replaceAll("\\s+", "_").toUpperCase();
                list.add(new Professeur(id, nom, prenom, sp));
            } catch (Exception e) {
                throw new ExcelParseException("Professeurs", i, e.getMessage(), e);
            }
        }
        return list;
    }

    private List<Salle> loadSallesDepuis(Workbook wb) {
        List<Salle> list = new ArrayList<>();
        Sheet sheet = requireSheet(wb, "Salles");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row)) continue;
            String nom = cell(row, 0);
            if (!nom.isBlank()) list.add(new Salle(nom));
        }
        return list;
    }

    private Configuration loadConfigurationDepuis(Workbook wb) {
        try {
            Sheet sheet = requireSheet(wb, "Configuration");
            Row row = sheet.getRow(1);
            if (row == null) return new Configuration(LocalDate.of(2026, 7, 1), 4);

            String dateStr = cell(row, 0).strip();
            LocalDate date;
            try { date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
            catch (Exception e) { date = LocalDate.of(2026, 7, 1); }

            int nbJours = 4;
            Cell nbCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (nbCell != null && nbCell.getCellType() == CellType.NUMERIC)
                nbJours = (int) nbCell.getNumericCellValue();

            LocalTime hDM = parseHeure(cell(row, 2), LocalTime.of(9,  0));
            LocalTime hFM = parseHeure(cell(row, 3), LocalTime.of(12, 0));
            LocalTime hDA = parseHeure(cell(row, 4), LocalTime.of(14, 0));
            LocalTime hFA = parseHeure(cell(row, 5), LocalTime.of(18, 0));

            return new Configuration(date, nbJours, hDM, hDA, hFM, hFA);
        } catch (Exception ex) {
            return new Configuration(LocalDate.of(2026, 7, 1), 4);
        }
    }

    private List<Groupe> loadGroupesDepuis(Workbook wb, List<Etudiant> etudiants) {
        // Index NOM_PRENOM → Etudiant pour récupérer le CNE
        Map<String, Etudiant> index = new HashMap<>();
        for (Etudiant e : etudiants)
            index.put((e.getNom() + "_" + e.getPrenom()).toUpperCase().trim(), e);

        List<Groupe> list = new ArrayList<>();
        Sheet sheet = requireSheet(wb, "Groupes");
        int startRow = isHeaderRow(sheet.getRow(0)) ? 2 : 1;

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row)) continue;
            try {
                String nom1    = cell(row, 0);
                String prenom1 = cell(row, 1);
                String nom2    = cell(row, 2);
                String prenom2 = cell(row, 3);
                Langue   langue = Langue.fromString(cell(row, 4));
                Filiere  fil    = FiliereRegistry.fromString(cell(row, 5));
                if (nom1.isBlank()) continue;

                Groupe g = new Groupe(langue, fil);

                Etudiant e1 = index.getOrDefault(
                    (nom1 + "_" + prenom1).toUpperCase().trim(),
                    new Etudiant("", nom1, prenom1, "", "", fil));
                g.addEtudiant(e1);

                if (!nom2.isBlank()) {
                    Etudiant e2 = index.getOrDefault(
                        (nom2 + "_" + prenom2).toUpperCase().trim(),
                        new Etudiant("", nom2, prenom2, "", "", fil));
                    g.addEtudiant(e2);
                }
                list.add(g);
            } catch (Exception e) {
                throw new ExcelParseException("Groupes", i, e.getMessage(), e);
            }
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODES INDIVIDUELLES — compatibilité ascendante (appellent loadAll)
    // ════════════════════════════════════════════════════════════════════════

    public List<Filiere>    loadFilieresDynamiques() { return loadAll().filieres; }
    public List<Etudiant>   loadEtudiants()          { return loadAll().etudiants; }
    public List<Professeur> loadProfesseurs()        { return loadAll().professeurs; }
    public List<Salle>      loadSalles()             { return loadAll().salles; }
    public Configuration    loadConfiguration()      { return loadAll().configuration; }
    public List<Groupe>     loadGroupes()            { return loadAll().groupes; }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private List<Filiere> filieresParDefaut() {
        return List.of(
            new Filiere("GI",   "Génie Informatique",            "INFORMATIQUE"),
            new Filiere("ID",   "Ingénierie des Données",         "INFORMATIQUE"),
            new Filiere("TDIA", "Transformation Digitale et IA", "MATHEMATIQUE")
        );
    }

    private Sheet requireSheet(Workbook wb, String name) {
        Sheet s = wb.getSheet(name);
        if (s != null) return s;
        // Cherche en normalisant les accents et la casse
        String target = normaliser(name);
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            if (normaliser(wb.getSheetName(i)).equals(target))
                return wb.getSheetAt(i);
        }
        throw new ExcelParseException(name, 0, "Feuille introuvable : " + name, null);
    }

    private String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase().trim()
            .replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
            .replace("à","a").replace("â","a").replace("ô","o").replace("î","i")
            .replace("ù","u").replace("ç","c");
    }

    private String genererCode(String libelle) {
        if (libelle == null || libelle.isBlank()) return "";
        String[] mots = libelle.trim().split("[\\s'\\-]+");
        StringBuilder sb = new StringBuilder();
        Set<String> articles = Set.of("ET","DES","DE","LE","LA","DU","EN","LES","UN","UNE");
        for (String mot : mots) {
            if (mot.isBlank()) continue;
            if (!articles.contains(mot.toUpperCase()))
                sb.append(Character.toUpperCase(mot.charAt(0)));
        }
        String code = sb.toString();
        return code.isEmpty()
            ? libelle.substring(0, Math.min(libelle.length(), 4)).toUpperCase()
            : code;
    }

    private String determinerSpecialite(String libelle) {
        if (libelle == null) return "INFORMATIQUE";
        String l = normaliser(libelle);
        if (l.contains("info") || l.contains("cyber") || l.contains("donnee") || l.contains("data"))
            return "INFORMATIQUE";
        if (l.contains("math") || l.contains("ia") || l.contains("artificiel"))
            return "MATHEMATIQUE";
        if (l.contains("gest") || l.contains("econom"))
            return "GESTION";
        if (l.contains("angl") || l.contains("langue"))
            return "ANGLAIS";
        return "INFORMATIQUE";
    }

    private boolean isHeaderRow(Row row) {
        if (row == null) return false;
        String v0 = cell(row, 0).toLowerCase();
        String v1 = cell(row, 1).toLowerCase();
        return v0.contains("code") || v0.contains("nom") || v0.contains("filiere")
            || v1.contains("libelle") || v1.contains("nom");
    }

    private LocalTime parseHeure(String s, LocalTime defaut) {
        if (s == null || s.isBlank()) return defaut;
        s = s.trim();
        try {
            if (s.contains(":"))
                return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
            if (s.matches("\\d{3,4}")) {
                int v = Integer.parseInt(s);
                return LocalTime.of(v / 100, v % 100);
            }
            if (s.matches("\\d{1,2}h\\d{2}")) {
                String[] p = s.split("h");
                return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            }
            double d = Double.parseDouble(s);
            int min = (int) Math.round(d * 24 * 60);
            return LocalTime.of(min / 60, min % 60);
        } catch (Exception e) { return defaut; }
    }

    private String cell(Row row, int col) {
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c))
                    yield c.getLocalDateTimeCellValue().toLocalDate()
                           .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                yield String.valueOf((long) c.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default      -> "";
        };
    }

    private boolean isBlank(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++)
            if (!cell(row, i).isBlank()) return false;
        return true;
    }
}
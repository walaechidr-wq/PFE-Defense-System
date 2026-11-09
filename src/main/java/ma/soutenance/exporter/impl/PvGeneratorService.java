package ma.soutenance.exporter.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import ma.soutenance.model.*;

import java.awt.Color;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


public class PvGeneratorService {

    private static final DateTimeFormatter FMT_DATE  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color C_PRIMAIRE   = new Color(0x1A, 0x3A, 0x6E); // bleu institutionnel foncé
    private static final Color C_FOND       = new Color(0xE8, 0xEF, 0xF8); // bleu très clair (fonds)
    private static final Color C_BORDURE    = new Color(0x9E, 0xB3, 0xD8); // bleu moyen (bordures)
    private static final Color C_TEXTE      = new Color(0x2C, 0x2C, 0x2C); // quasi-noir
    private static final Color C_VIDE       = new Color(0xFA, 0xFA, 0xFA); // blanc cassé (zones à remplir)
    private static final Color C_PLACEHOLDER= new Color(0xC0, 0xC0, 0xC0); // gris clair (texte placeholder)

    private static final Font F_SECTION  = new Font(Font.HELVETICA,  8.5f, Font.BOLD,   C_PRIMAIRE);
    private static final Font F_LABEL    = new Font(Font.HELVETICA,  8f,   Font.BOLD,   C_TEXTE);
    private static final Font F_VALEUR   = new Font(Font.HELVETICA,  9f,   Font.NORMAL, C_TEXTE);
    private static final Font F_JURY_NOM = new Font(Font.HELVETICA,  8.5f, Font.BOLD,   C_PRIMAIRE);
    private static final Font F_JURY_ROLE= new Font(Font.HELVETICA,  7f,   Font.ITALIC, C_BORDURE);
    private static final Font F_JURY_SPEC= new Font(Font.HELVETICA,  7f,   Font.ITALIC, C_BORDURE);
    private static final Font F_PIED     = new Font(Font.HELVETICA,  7f,   Font.ITALIC, Color.GRAY);

   
    public void generatePV(Etudiant etudiant,
            Soutenance soutenance,
            String outputPath) throws IOException {

    	Document doc = new Document(PageSize.A4, 50, 50, 45, 45);
    	FileOutputStream fos = null;
    	PdfWriter writer = null;

    	try {
    		fos    = new FileOutputStream(outputPath);
    		writer = PdfWriter.getInstance(doc, fos);
    		doc.open();
    		buildContenu(doc, etudiant, soutenance);
    	} catch (DocumentException e) {
    		throw new IOException("Erreur génération PV : " + e.getMessage(), e);
    	} finally {
    		// ← Fermeture explicite et garantie
    		if (doc.isOpen()) doc.close();
    		if (writer != null) writer.close();
    		if (fos != null) {
    			try { fos.flush(); fos.close(); } catch (IOException ignored) {}
    		}
    	}
    }

   
    public void generateAllPV(Planning planning, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Impossible de créer le répertoire : " + outputDir);

        for (Soutenance soutenance : planning.getSoutenances()) {
            for (Etudiant etudiant : soutenance.getGroupe().getEtudiants()) {
                String path = outputDir + File.separator + nomFichier(etudiant);
                try {
                    generatePV(etudiant, soutenance, path);
                    System.out.println("[PV] OK → " + path);
                } catch (Exception ex) {
                    System.err.println("[PV] ERREUR pour " + etudiant.getFullName()
                        + " : " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }


    private void buildContenu(Document   doc,
                              Etudiant   etudiant,
                              Soutenance soutenance) throws DocumentException {

        
        doc.add(buildEnTete(etudiant, soutenance));

        doc.add(saut(10));
        doc.add(titreSection("INFORMATIONS ÉTUDIANT"));
        doc.add(buildTableInfosEtudiant(etudiant));

        doc.add(saut(10));
        doc.add(titreSection("INFORMATIONS SOUTENANCE"));
        doc.add(buildTableInfosSoutenance(soutenance));

        doc.add(saut(12));
        doc.add(titreSection("SUJET DE LA SOUTENANCE"));
        doc.add(buildZoneVide(115, "Intitulé du projet / sujet  —  à compléter par le jury"));

        doc.add(saut(10));
        doc.add(titreSection("APPRÉCIATION ET RÉSULTATS"));
        doc.add(buildZoneMoyenne());

        doc.add(saut(12));
        doc.add(titreSection("JURY ET SIGNATURES"));
        doc.add(buildTableJury(soutenance));

        doc.add(saut(18));
        doc.add(buildPiedDePage(etudiant));
    }


    private PdfPTable buildEnTete(Etudiant e, Soutenance s) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell cTitre = new PdfPCell();
        cTitre.setBackgroundColor(C_PRIMAIRE);
        cTitre.setBorder(Rectangle.NO_BORDER);
        cTitre.setPaddingTop(14);
        cTitre.setPaddingBottom(14);
        cTitre.setPaddingLeft(16);
        cTitre.setPaddingRight(16);
        cTitre.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph titreP = new Paragraph();
        titreP.setAlignment(Element.ALIGN_CENTER);
        titreP.add(new Chunk("PROCÈS-VERBAL DE SOUTENANCE\n",
            new Font(Font.HELVETICA, 15f, Font.BOLD, Color.WHITE)));
        titreP.add(new Chunk("Projet de Fin d'Études  —  " + filiereLabel(e),
            new Font(Font.HELVETICA, 9f, Font.ITALIC, new Color(0xB0, 0xC8, 0xEA))));
        cTitre.addElement(titreP);
        t.addCell(cTitre);

        PdfPTable sub = new PdfPTable(new float[]{1, 1});
        sub.setWidthPercentage(100);

        String dateSout = (s.getCreneau() != null && s.getCreneau().getDate() != null)
            ? s.getCreneau().getDate().format(FMT_DATE) : "—";

        PdfPCell cRef  = miniCell("Référence PV : " + s.getId(), Element.ALIGN_LEFT);
        PdfPCell cDate = miniCell("Date soutenance : " + dateSout, Element.ALIGN_RIGHT);
        sub.addCell(cRef);
        sub.addCell(cDate);

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setBorder(Rectangle.NO_BORDER);
        cSub.setPadding(0);
        t.addCell(cSub);

        return t;
    }


    private PdfPTable buildTableInfosEtudiant(Etudiant e) throws DocumentException {
        PdfPTable t = grille4colonnes();
        ajouterLignePaire(t, "Nom",     e.getNom(),
                             "Prénom",  e.getPrenom());
        ajouterLignePaire(t, "CNE",     e.getCne(),
                             "Filière", filiereLabel(e));
        return t;
    }


    private PdfPTable buildTableInfosSoutenance(Soutenance s) throws DocumentException {
        Creneau c = s.getCreneau();
        String date  = (c != null && c.getDate()       != null) ? c.getDate().format(FMT_DATE) : "—";
        String debut = (c != null && c.getHeureDebut() != null) ? c.getHeureDebut().format(FMT_HEURE) : "—";
        String fin   = (c != null && c.getHeureFin()   != null) ? c.getHeureFin().format(FMT_HEURE)   : "—";
        String heure = debut + " – " + fin;
        String salle = s.getSalleNom();
        String enc   = s.getEncadrant() != null ? s.getEncadrant().getFullName() : "—";

        PdfPTable t = grille4colonnes();
        ajouterLignePaire(t, "Date",      date,  "Heure",     heure);
        ajouterLignePaire(t, "Salle",     salle, "Encadrant", enc);

        if (s.getGroupe() != null && s.getGroupe().isBinome()) {
            PdfPCell note = new PdfPCell(new Phrase(
                "  Soutenance en binôme — ce PV est individuel.",
                new Font(Font.HELVETICA, 7.5f, Font.ITALIC, C_PRIMAIRE)));
            note.setColspan(4);
            note.setBackgroundColor(C_FOND);
            note.setBorderColor(C_BORDURE);
            note.setBorderWidth(0.5f);
            note.setPadding(5);
            t.addCell(note);
        }

        return t;
    }


    private PdfPTable buildZoneVide(float hauteur, String placeholder) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(3);

        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(hauteur);
        cell.setBackgroundColor(C_VIDE);
        cell.setBorderColor(C_BORDURE);
        cell.setBorderWidth(0.8f);
        cell.setPadding(10);

        Paragraph ph = new Paragraph(placeholder,
            new Font(Font.HELVETICA, 8f, Font.ITALIC, C_PLACEHOLDER));
        ph.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(ph);
        t.addCell(cell);
        return t;
    }


    private PdfPTable buildZoneMoyenne() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.8f, 2f, 1.2f, 1.2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(3);

        t.addCell(labelCell("Mention"));
        PdfPCell cMention = videCell(38);
        cMention.setColspan(1);
        t.addCell(cMention);

        t.addCell(labelCell("Moyenne  / 20"));
        t.addCell(videCell(38));

        return t;
    }


    private PdfPTable buildTableJury(Soutenance s) throws DocumentException {
        List<Professeur> jury = s.getJuryComplet(); // encadrant + jusqu'à 2 autres
        int nb = Math.min(3, jury.size());
        if (nb == 0) nb = 1;

        float[] widths = new float[nb];
        Arrays.fill(widths, 1f);

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);

        String[] roles = {"Encadrant", "Jury 1", "Jury 2"};

        for (int i = 0; i < nb; i++) {
            Professeur p = i < jury.size() ? jury.get(i) : null;

            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(C_FOND);
            cell.setBorderColor(C_BORDURE);
            cell.setBorderWidth(0.6f);
            cell.setPaddingTop(8);
            cell.setPaddingBottom(8);
            cell.setPaddingLeft(10);
            cell.setPaddingRight(10);

            Paragraph para = new Paragraph();
            para.add(new Chunk(roles[i] + "\n", F_JURY_ROLE));
            para.add(new Chunk(p != null ? p.getFullName() : "—", F_JURY_NOM));
            if (p != null && p.getSpecialite() != null) {
                para.add(new Chunk("\n" + p.getSpecialite().getLibelle(), F_JURY_SPEC));
            }
            cell.addElement(para);
            t.addCell(cell);
        }

        for (int i = 0; i < nb; i++) {
            PdfPCell sig = new PdfPCell();
            sig.setFixedHeight(65);
            sig.setBackgroundColor(C_VIDE);
            sig.setBorderColor(C_BORDURE);
            sig.setBorderWidth(0.6f);
            sig.setPadding(6);

            Paragraph sigLabel = new Paragraph("Signature",
                new Font(Font.HELVETICA, 7f, Font.ITALIC, C_PLACEHOLDER));
            sigLabel.setAlignment(Element.ALIGN_CENTER);
            sig.addElement(sigLabel);
            t.addCell(sig);
        }

        return t;
    }


    private PdfPTable buildPiedDePage(Etudiant e) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthTop(0.5f);
        cell.setBorderWidthBottom(0f);
        cell.setBorderWidthLeft(0f);
        cell.setBorderWidthRight(0f);
        cell.setBorderColorTop(C_BORDURE);
        cell.setPaddingTop(7);
        cell.setPaddingBottom(0);

        String txt = "Système de gestion des soutenances PFE  ·  " +
                     "Étudiant : " + e.getFullName() + "  ·  CNE : " + e.getCne();
        cell.addElement(new Paragraph(txt, F_PIED));
        t.addCell(cell);
        return t;
    }


    /** Titre de section avec ligne de séparation en dessous. */
    private Paragraph titreSection(String texte) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2);
        p.setSpacingAfter(3);
        p.add(new Chunk(texte, F_SECTION));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(new LineSeparator(0.5f, 100f, C_BORDURE, Element.ALIGN_LEFT, -2f)));
        return p;
    }

    /** Espace vertical (simulation de padding). */
    private Paragraph saut(float hauteur) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(hauteur);
        return p;
    }

    /** Grille à 4 colonnes label/valeur/label/valeur. */
    private PdfPTable grille4colonnes() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1f, 2f, 1f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(3);
        return t;
    }

    /** Ajoute une ligne label|valeur|label|valeur dans un tableau 4 colonnes. */
    private void ajouterLignePaire(PdfPTable t,
                                    String l1, String v1,
                                    String l2, String v2) {
        t.addCell(labelCell(l1));
        t.addCell(valeurCell(v1));
        t.addCell(labelCell(l2));
        t.addCell(valeurCell(v2));
    }

    private PdfPCell labelCell(String label) {
        PdfPCell c = new PdfPCell(new Phrase(label, F_LABEL));
        c.setBackgroundColor(C_FOND);
        c.setPadding(6);
        c.setBorderColor(C_BORDURE);
        c.setBorderWidth(0.5f);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private PdfPCell valeurCell(String valeur) {
        PdfPCell c = new PdfPCell(new Phrase(valeur != null ? valeur : "—", F_VALEUR));
        c.setBackgroundColor(Color.WHITE);
        c.setPadding(6);
        c.setBorderColor(C_BORDURE);
        c.setBorderWidth(0.5f);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private PdfPCell videCell(float hauteur) {
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(hauteur);
        c.setBackgroundColor(C_VIDE);
        c.setBorderColor(C_BORDURE);
        c.setBorderWidth(0.8f);
        return c;
    }

    private PdfPCell miniCell(String texte, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texte,
            new Font(Font.HELVETICA, 7.5f, Font.NORMAL, Color.GRAY)));
        c.setBackgroundColor(C_FOND);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(C_BORDURE);
        c.setBorderWidthBottom(0.5f);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        return c;
    }

    private String filiereLabel(Etudiant e) {
        return e.getFiliere() != null ? e.getFiliere().getLibelle() : "—";
    }

    /** Nom de fichier PDF propre et sans caractères spéciaux. */
    private String nomFichier(Etudiant e) {
        return "PV_" + sanitize(e.getNom()) + "_" + sanitize(e.getPrenom())
             + "_" + sanitize(e.getCne()) + ".pdf";
    }

    private String sanitize(String s) {
        if (s == null) return "INCONNU";
        return s.trim().toUpperCase()
                .replaceAll("[éèêëÉÈÊ]", "E")
                .replaceAll("[àâäÀÂ]",   "A")
                .replaceAll("[ùûüÙÛ]",   "U")
                .replaceAll("[îïÎÏ]",     "I")
                .replaceAll("[ôöÔÖ]",     "O")
                .replaceAll("[ç Ç]",      "C")
                .replaceAll("[^A-Z0-9]",  "_")
                .replaceAll("_+",         "_");
    }
}
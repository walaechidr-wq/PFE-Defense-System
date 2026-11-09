package ma.soutenance.history;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.soutenance.model.Planning;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class HistoryManager {

    public static class EntreeInfo {
        public String        id;
        public LocalDateTime dateGeneration;
        public int           nbSoutenances;
        public int           nbJours;
        public String        dateDebut;
        public String        cheminExcel;
        public String        cheminPdf;
        public boolean       conforme;
        public String        resumeValidation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class HistoryEntry {
        public String        id;
        public LocalDateTime dateGeneration;
        public int           nbSoutenances;
        public int           nbJours;
        public String        dateDebut;
        public String        cheminExcel  = "";
        public String        cheminPdf    = "";
        public boolean       conforme;
        public String        resumeValidation = "";
    }

    private static final String ROOT  = "historique";
    private static final String INDEX = ROOT + "/index.json";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper mapper;

    public HistoryManager() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try { Files.createDirectories(Path.of(ROOT)); } catch (IOException ignored) {}
    }

    public EntreeInfo sauvegarder(Planning planning,
                                   String srcExcel, String srcPdf,
                                   boolean conforme, String resume) throws IOException {
        String ts  = planning.getDateGeneration().format(TS);
        String dir = ROOT + "/" + ts;
        Files.createDirectories(Path.of(dir));

        String destExcel = "", destPdf = "";
        if (srcExcel != null && new File(srcExcel).exists()) {
            destExcel = dir + "/affectation_" + ts + ".xlsx";
            Files.copy(Path.of(srcExcel), Path.of(destExcel), StandardCopyOption.REPLACE_EXISTING);
        }
        if (srcPdf != null && new File(srcPdf).exists()) {
            destPdf = dir + "/planning_" + ts + ".pdf";
            Files.copy(Path.of(srcPdf), Path.of(destPdf), StandardCopyOption.REPLACE_EXISTING);
        }

        HistoryEntry e = new HistoryEntry();
        e.id               = planning.getId();
        e.dateGeneration   = planning.getDateGeneration();
        e.nbSoutenances    = planning.getSoutenances().size();
        e.dateDebut        = planning.getConfiguration() != null ? planning.getConfiguration().getDateDebut().toString() : "";
        e.nbJours          = planning.getConfiguration() != null ? planning.getConfiguration().getNombreJours() : 0;
        e.cheminExcel      = destExcel;
        e.cheminPdf        = destPdf;
        e.conforme         = conforme;
        e.resumeValidation = resume;

        List<HistoryEntry> all = readIndex();
        all.add(0, e);
        mapper.writeValue(new File(INDEX), all);

        return toInfo(e);
    }

    public List<EntreeInfo> listerEntrees() {
        List<EntreeInfo> result = new ArrayList<>();
        for (HistoryEntry e : readIndex()) result.add(toInfo(e));
        return result;
    }

    public void supprimer(String id) throws IOException {
        List<HistoryEntry> all = readIndex();
        all.removeIf(e -> id.equals(e.id));
        mapper.writeValue(new File(INDEX), all);
    }

    public void viderTout() throws IOException {
        mapper.writeValue(new File(INDEX), new ArrayList<>());
    }

    public String getHistoRootPath() {
        return Path.of(ROOT).toAbsolutePath().toString();
    }

    private List<HistoryEntry> readIndex() {
        File f = new File(INDEX);
        if (!f.exists()) return new ArrayList<>();
        try { return mapper.readValue(f, new TypeReference<List<HistoryEntry>>() {}); }
        catch (IOException ignored) { return new ArrayList<>(); }
    }

    private EntreeInfo toInfo(HistoryEntry e) {
        EntreeInfo i = new EntreeInfo();
        i.id               = e.id;
        i.dateGeneration   = e.dateGeneration;
        i.nbSoutenances    = e.nbSoutenances;
        i.nbJours          = e.nbJours;
        i.dateDebut        = e.dateDebut;
        i.cheminExcel      = e.cheminExcel;
        i.cheminPdf        = e.cheminPdf;
        i.conforme         = e.conforme;
        i.resumeValidation = e.resumeValidation;
        return i;
    }
}

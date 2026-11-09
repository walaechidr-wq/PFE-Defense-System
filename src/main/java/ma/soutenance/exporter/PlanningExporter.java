package ma.soutenance.exporter;
import ma.soutenance.model.Planning;
import java.io.IOException;
public interface PlanningExporter {
    void export(Planning planning, String outputPath) throws IOException;
    String getFormat();
}

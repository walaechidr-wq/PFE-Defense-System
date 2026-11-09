package ma.soutenance.repository;
import java.util.List;

public interface DataRepository<T> {
    List<T> readAll();
    void writeAll(List<T> data);
}

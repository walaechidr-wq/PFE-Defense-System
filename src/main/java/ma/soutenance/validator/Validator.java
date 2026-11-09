package ma.soutenance.validator;
import ma.soutenance.model.Planning;
import java.util.List;
public interface Validator {
    boolean validate(Planning planning);
    List<String> getErrors();
    String getNom();
}

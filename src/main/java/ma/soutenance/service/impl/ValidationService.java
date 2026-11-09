package ma.soutenance.service.impl;

import ma.soutenance.model.Planning;
import ma.soutenance.validator.Validator;

import java.util.ArrayList;
import java.util.List;


public class ValidationService {

    private final List<Validator> validators;

    public ValidationService(List<Validator> validators) {
        this.validators = List.copyOf(validators);
    }

    public ValidationResult valider(Planning planning) {
        List<ErreurValidator> allErrors = new ArrayList<>();
        for (Validator v : validators) {
            boolean ok = v.validate(planning);
            if (!ok)
                v.getErrors().forEach(e -> allErrors.add(new ErreurValidator(v.getNom(), e)));
        }
        return new ValidationResult(allErrors.isEmpty(), allErrors);
    }

    public static class ValidationResult {
        private final boolean valide;
        private final List<ErreurValidator> erreurs;

        public ValidationResult(boolean valide, List<ErreurValidator> erreurs) {
            this.valide  = valide;
            this.erreurs = erreurs;
        }

        public boolean isValide()                 { return valide; }
        public List<ErreurValidator> getErreurs() { return erreurs; }
        public int getNbErreurs()                 { return erreurs.size(); }

        public String getResumeCourt() {
            return valide
                ? " Planning conforme — aucune erreur détectée"
                : erreurs.size() + " erreur(s) détectée(s)";
        }
    }

    public static class ErreurValidator {
        private final String validator;
        private final String message;
        public ErreurValidator(String validator, String message) {
            this.validator = validator;
            this.message   = message;
        }
        public String getValidator() { return validator; }
        public String getMessage()   { return message; }
        @Override public String toString() { return "[" + validator + "] " + message; }
    }
}

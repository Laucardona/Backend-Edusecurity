package edusecurity.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        List<String> faltantes = PoliticaPassword.requisitosFaltantes(value);
        if (faltantes.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "la contraseña debe tener " + String.join(", ", faltantes))
                .addConstraintViolation();
        return false;
    }
}

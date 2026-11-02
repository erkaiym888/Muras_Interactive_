package kg.murasinteractive.user.exception;

/** Ошибка в конкретном поле формы (например, слишком длинный пароль). */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}

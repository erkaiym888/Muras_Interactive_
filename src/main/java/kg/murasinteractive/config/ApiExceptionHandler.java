package kg.murasinteractive.config;

import kg.murasinteractive.user.exception.EmailAlreadyUsedException;
import kg.murasinteractive.user.exception.FieldValidationException;
import kg.murasinteractive.user.exception.InvalidResetTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Превращает ошибки в понятный JSON: {"errors": {"поле": "сообщение"}} или {"message": "..."}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Map<String, Object>> field(FieldValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("errors", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> emailUsed(EmailAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errors", Map.of("email", ex.getMessage())));
    }

    /** 404, 409 и другие ошибки с текстом для пользователя (например, "В категории есть обряды"). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        String reason = ex.getReason() == null ? "" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", reason));
    }

    /** Тело запроса не JSON или поле неверного типа. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Неверный формат данных"));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<Map<String, Object>> badToken(InvalidResetTokenException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}

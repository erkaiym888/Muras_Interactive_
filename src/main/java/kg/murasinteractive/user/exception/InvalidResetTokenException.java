package kg.murasinteractive.user.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Ссылка недействительна или устарела. Запросите восстановление пароля заново.");
    }
}

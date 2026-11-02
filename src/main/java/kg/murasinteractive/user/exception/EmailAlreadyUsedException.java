package kg.murasinteractive.user.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("Этот адрес уже зарегистрирован");
    }
}

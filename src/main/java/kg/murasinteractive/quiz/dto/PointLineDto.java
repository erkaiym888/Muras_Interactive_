package kg.murasinteractive.quiz.dto;

/** Строка расшифровки очков: за что и сколько начислено. */
public record PointLineDto(String code, String label, int points) {
}

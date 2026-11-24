package kg.murasinteractive.rating.dto;

/** Строка рейтинга: место, отображаемое имя, уровень и очки. me - это текущий пользователь. */
public record RatingRowDto(int place, String displayName, int level, String levelTitle, int points, boolean me) {
}

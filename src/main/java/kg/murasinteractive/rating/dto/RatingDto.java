package kg.murasinteractive.rating.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Рейтинг (FR-5.5, Приложение Б.4). period: all - за всё время, week - за текущую неделю
 * (с понедельника по воскресенье по Бишкеку). me - строка текущего пользователя, если он не попал в список.
 */
public record RatingDto(String period, LocalDate weekStart, LocalDate weekEnd, int participants,
                        List<RatingRowDto> rows, RatingRowDto me) {
}

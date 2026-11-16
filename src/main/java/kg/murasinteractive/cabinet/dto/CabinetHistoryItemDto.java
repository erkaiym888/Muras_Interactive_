package kg.murasinteractive.cabinet.dto;

import java.time.Instant;

/** Одно прохождение викторины. Заполняется историей прохождений (FR-5.4). */
public record CabinetHistoryItemDto(String quizTitle, int scorePercent, int pointsEarned, Instant completedAt) {
}

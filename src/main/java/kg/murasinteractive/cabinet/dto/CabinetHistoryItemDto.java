package kg.murasinteractive.cabinet.dto;

import java.time.Instant;

/** Одно прохождение викторины. Заполняется историей прохождений (FR-5.4); attemptId - для разбора ответов. */
public record CabinetHistoryItemDto(Long attemptId, Long quizId, String quizTitle, int scorePercent,
                                    int pointsEarned, boolean repeatAttempt, Instant completedAt) {
}

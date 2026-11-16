package kg.murasinteractive.quiz.dto;

import java.time.Instant;

/** Строка истории прохождений (FR-5.4). */
public record HistoryItemDto(Long attemptId, Long quizId, String quizTitle, String topic, int score,
                             int correctCount, int questionCount, int pointsEarned, boolean repeatAttempt,
                             Instant completedAt) {
}

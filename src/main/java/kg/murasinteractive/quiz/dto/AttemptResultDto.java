package kg.murasinteractive.quiz.dto;

import java.time.Instant;
import java.util.List;

/** Итог прохождения: процент, очки с расшифровкой и разбор каждого вопроса. */
public record AttemptResultDto(Long attemptId, Long quizId, String quizTitle, String topic,
                               int score, int correctCount, int questionCount, int hintsUsed, int bestStreak,
                               boolean repeatAttempt, int pointsEarned, int totalPoints,
                               List<PointLineDto> points, List<QuestionResultDto> questions,
                               Instant completedAt) {
}

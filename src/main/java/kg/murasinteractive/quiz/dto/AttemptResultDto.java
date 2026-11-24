package kg.murasinteractive.quiz.dto;

import kg.murasinteractive.awards.dto.AchievementDto;
import kg.murasinteractive.awards.dto.LevelDto;

import java.time.Instant;
import java.util.List;

/**
 * Итог прохождения: процент, очки с расшифровкой и разбор каждого вопроса.
 * newAchievements и levelUp (FR-5.3) - значки и новый уровень, полученные этим прохождением;
 * в истории (FR-5.4) они пустые.
 */
public record AttemptResultDto(Long attemptId, Long quizId, String quizTitle, String topic,
                               int score, int correctCount, int questionCount, int hintsUsed, int bestStreak,
                               boolean repeatAttempt, int pointsEarned, int totalPoints,
                               List<PointLineDto> points, List<QuestionResultDto> questions,
                               Instant completedAt, List<AchievementDto> newAchievements, LevelDto levelUp) {
}

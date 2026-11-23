package kg.murasinteractive.quiz.dto;

import java.time.Instant;
import java.util.List;

/**
 * Прогресс пользователя (FR-5.4): общие цифры, прогресс по темам и по каждой викторине.
 * Лучший результат викторины - максимум по всем её прохождениям.
 */
public record ProgressDto(int totalPoints, int quizzesTotal, int quizzesCompleted, int attemptsCount,
                          int averageBestScore, int correctAnswers, int answeredQuestions, int bestStreak,
                          List<Topic> topics, List<QuizProgress> quizzes) {

    public record Topic(String topic, int quizzesTotal, int quizzesCompleted, int averageBestScore) {
    }

    /** bestScore и lastScore = null, если викторина ещё не пройдена. */
    public record QuizProgress(Long quizId, String title, String topic, String difficulty, int attempts,
                               Integer bestScore, Integer lastScore, Instant lastCompletedAt, int pointsEarned) {
    }
}

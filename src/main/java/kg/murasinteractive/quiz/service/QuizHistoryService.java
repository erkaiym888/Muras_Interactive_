package kg.murasinteractive.quiz.service;

import kg.murasinteractive.points.model.PointTransaction;
import kg.murasinteractive.points.repository.PointTransactionRepository;
import kg.murasinteractive.quiz.dto.AttemptResultDto;
import kg.murasinteractive.quiz.dto.HistoryItemDto;
import kg.murasinteractive.quiz.dto.PointLineDto;
import kg.murasinteractive.quiz.dto.ProgressDto;
import kg.murasinteractive.quiz.model.Quiz;
import kg.murasinteractive.quiz.model.QuizAttempt;
import kg.murasinteractive.quiz.model.QuizTopic;
import kg.murasinteractive.quiz.repository.QuizAttemptRepository;
import kg.murasinteractive.quiz.repository.QuizRepository;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * История прохождений и прогресс пользователя (FR-5.4). Данные берутся из quiz_attempt,
 * разбор ответов - из quiz_attempt_answer, расшифровка очков - из point_transaction.
 * Каждый видит только свою историю.
 */
@Service
public class QuizHistoryService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final PointTransactionRepository transactionRepository;

    public QuizHistoryService(QuizAttemptRepository attemptRepository, QuizRepository quizRepository,
                              PointTransactionRepository transactionRepository) {
        this.attemptRepository = attemptRepository;
        this.quizRepository = quizRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Все прохождения, новые сверху; quizId - только по одной викторине. */
    @Transactional(readOnly = true)
    public List<HistoryItemDto> history(Long userId, Long quizId) {
        return attemptRepository.findByUser_UserIdOrderByCompletedAtDesc(userId).stream()
                .filter(a -> quizId == null || quizId.equals(a.getQuiz().getQuizId()))
                .map(QuizHistoryService::toItem)
                .toList();
    }

    /** Разбор одного прохождения: ответы, правильные варианты, пояснения и начисленные очки. */
    @Transactional(readOnly = true)
    public AttemptResultDto attempt(UserAccount user, Long attemptId) {
        QuizAttempt attempt = attemptRepository.findByAttemptIdAndUser_UserId(attemptId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Прохождение не найдено"));
        List<PointLineDto> lines = transactionRepository
                .findBySourceIdAndSourceTypeStartingWithOrderByTransactionIdAsc(attemptId, "QUIZ_").stream()
                .map(t -> new PointLineDto(t.getSourceType(), label(t, attempt), t.getPointsDelta()))
                .toList();
        return QuizScoringService.toResult(attempt, lines, user.getTotalPoints());
    }

    /** Прогресс: сколько викторин пройдено, лучшие результаты по темам и по каждой викторине. */
    @Transactional(readOnly = true)
    public ProgressDto progress(UserAccount user) {
        List<QuizAttempt> attempts = attemptRepository.findByUser_UserIdOrderByCompletedAtDesc(user.getUserId());
        Map<Long, List<QuizAttempt>> byQuiz = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getQuiz().getQuizId()));

        List<ProgressDto.QuizProgress> quizzes = new ArrayList<>();
        for (Quiz quiz : quizRepository.findAllByOrderByQuizIdAsc()) {
            List<QuizAttempt> list = byQuiz.getOrDefault(quiz.getQuizId(), List.of());
            Integer best = list.stream().map(QuizAttempt::getScore).max(Integer::compare).orElse(null);
            QuizAttempt last = list.isEmpty() ? null : list.get(0);
            int points = list.stream().mapToInt(QuizAttempt::getPointsEarned).sum();
            quizzes.add(new ProgressDto.QuizProgress(quiz.getQuizId(), quiz.getTitle(), quiz.getTopic().name(),
                    quiz.getDifficulty().name(), list.size(), best, last == null ? null : last.getScore(),
                    last == null ? null : last.getCompletedAt(), points));
        }

        List<ProgressDto.Topic> topics = new ArrayList<>();
        for (QuizTopic topic : QuizTopic.values()) {
            List<ProgressDto.QuizProgress> inTopic = quizzes.stream()
                    .filter(q -> q.topic().equals(topic.name())).toList();
            topics.add(new ProgressDto.Topic(topic.name(), inTopic.size(),
                    (int) inTopic.stream().filter(q -> q.bestScore() != null).count(), averageBest(inTopic)));
        }

        int completed = (int) quizzes.stream().filter(q -> q.bestScore() != null).count();
        int correct = attempts.stream().mapToInt(QuizAttempt::getCorrectCount).sum();
        int answered = attempts.stream().mapToInt(QuizAttempt::getQuestionCount).sum();
        int streak = attempts.stream().mapToInt(QuizAttempt::getBestStreak).max().orElse(0);
        return new ProgressDto(user.getTotalPoints(), quizzes.size(), completed, attempts.size(),
                averageBest(quizzes), correct, answered, streak, topics, quizzes);
    }

    /** Средний лучший результат по пройденным викторинам; 0, если ни одна не пройдена. */
    private static int averageBest(List<ProgressDto.QuizProgress> quizzes) {
        return (int) Math.round(quizzes.stream()
                .filter(q -> q.bestScore() != null)
                .mapToInt(ProgressDto.QuizProgress::bestScore)
                .average().orElse(0));
    }

    public static HistoryItemDto toItem(QuizAttempt a) {
        Quiz quiz = a.getQuiz();
        return new HistoryItemDto(a.getAttemptId(), quiz.getQuizId(), quiz.getTitle(), quiz.getTopic().name(),
                a.getScore(), a.getCorrectCount(), a.getQuestionCount(), a.getPointsEarned(), a.isRepeatAttempt(),
                a.getCompletedAt());
    }

    private static String label(PointTransaction t, QuizAttempt attempt) {
        return switch (t.getSourceType()) {
            case "QUIZ_ANSWERS" -> attempt.isRepeatAttempt()
                    ? "Ответы на вопросы (повторное прохождение, очки × 0,5)" : "Ответы на вопросы";
            case "QUIZ_STREAK" -> "Серия из " + attempt.getBestStreak() + " верных ответов подряд";
            case "QUIZ_COMPLETE" -> "Завершение викторины";
            case "QUIZ_RESULT" -> attempt.getScore() >= 100 ? "Результат 100%" : "Результат 80% и выше";
            case "QUIZ_FIRST_PASS" -> "Первое прохождение викторины";
            default -> t.getSourceType();
        };
    }
}

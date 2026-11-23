package kg.murasinteractive.quiz.service;

import kg.murasinteractive.points.service.PointRules;
import kg.murasinteractive.points.service.PointService;
import kg.murasinteractive.quiz.dto.AttemptRequest;
import kg.murasinteractive.quiz.dto.AttemptResultDto;
import kg.murasinteractive.quiz.dto.HintDto;
import kg.murasinteractive.quiz.dto.PointLineDto;
import kg.murasinteractive.quiz.dto.QuestionResultDto;
import kg.murasinteractive.quiz.model.AnswerOption;
import kg.murasinteractive.quiz.model.QuestionType;
import kg.murasinteractive.quiz.model.Quiz;
import kg.murasinteractive.quiz.model.QuizAttempt;
import kg.murasinteractive.quiz.model.QuizAttemptAnswer;
import kg.murasinteractive.quiz.model.QuizQuestion;
import kg.murasinteractive.quiz.repository.QuizAttemptRepository;
import kg.murasinteractive.quiz.repository.QuizRepository;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Проверка ответов и начисление очков (FR-5.2) по правилам Приложения Б.1 ТЗ:
 * <ul>
 *   <li>правильный ответ - базовые очки вопроса (поле points: 10 / 15 / 20);
 *       при частично верном ответе (несколько ответов, сопоставление) - пропорционально числу верных пунктов;</li>
 *   <li>подсказка - очки за вопрос × 0,5;</li>
 *   <li>серия верных ответов подряд: 3 - +5, 5 и более - +10 (один раз за прохождение);</li>
 *   <li>завершение викторины - +20, не более 3 раз в сутки на одну викторину (сутки по Бишкеку);</li>
 *   <li>результат 80% и выше - +20, 100% - +50 вместо 20;</li>
 *   <li>первое прохождение викторины - +25;</li>
 *   <li>повторное прохождение - очки за вопросы × 0,5, бонусы за результат не начисляются.</li>
 * </ul>
 * Числа берутся из справочника point_rule (см. PointRules).
 */
@Service
public class QuizScoringService {

    public static final ZoneId BISHKEK = ZoneId.of("Asia/Bishkek");

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserAccountRepository userRepository;
    private final PointRules rules;
    private final PointService pointService;

    public QuizScoringService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
                              UserAccountRepository userRepository, PointRules rules, PointService pointService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.rules = rules;
        this.pointService = pointService;
    }

    /** Проверяет ответы, сохраняет прохождение и начисляет очки. */
    @Transactional
    public AttemptResultDto submit(Long userId, Long quizId, AttemptRequest request) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Quiz quiz = findQuiz(quizId);
        if (quiz.getQuestions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В викторине нет вопросов");
        }

        boolean repeat = attemptRepository.existsByUser_UserIdAndQuiz_QuizId(userId, quizId);
        long completedToday = attemptRepository.countByUser_UserIdAndQuiz_QuizIdAndCompletedAtGreaterThanEqual(
                userId, quizId, startOfToday());

        Map<Long, AttemptRequest.Answer> answers = new LinkedHashMap<>();
        for (AttemptRequest.Answer a : request.answers()) {
            answers.putIfAbsent(a.questionId(), a);
        }

        double hintMultiplier = rules.value(PointRules.HINT_MULTIPLIER);
        double repeatMultiplier = repeat ? rules.value(PointRules.REPEAT_MULTIPLIER) : 1.0;

        QuizAttempt attempt = new QuizAttempt(user, quiz);
        double fractionSum = 0;
        int questionPoints = 0, correctCount = 0, hints = 0, streak = 0, bestStreak = 0;
        int order = 1;
        for (QuizQuestion question : quiz.getQuestions()) {
            AttemptRequest.Answer answer = answers.get(question.getQuestionId());
            boolean hintUsed = answer != null && answer.hintUsed() && question.getQuestionType() != QuestionType.TRUE_FALSE;
            Graded g = grade(question, answer);

            double fraction = g.total == 0 ? 0 : (double) g.correct / g.total;
            boolean fullyCorrect = g.total > 0 && g.correct == g.total;
            int points = (int) Math.round(question.getPoints() * fraction
                    * (hintUsed ? hintMultiplier : 1.0) * repeatMultiplier);

            fractionSum += fraction;
            questionPoints += points;
            if (hintUsed) hints++;
            if (fullyCorrect) {
                correctCount++;
                bestStreak = Math.max(bestStreak, ++streak);
            } else {
                streak = 0;
            }

            QuizAttemptAnswer row = new QuizAttemptAnswer();
            row.setQuestionId(question.getQuestionId());
            row.setSortOrder(order++);
            row.setQuestionText(question.getQuestionText());
            row.setQuestionType(question.getQuestionType().name());
            row.setCorrectItems(g.correct);
            row.setTotalItems(g.total);
            row.setCorrect(fullyCorrect);
            row.setHintUsed(hintUsed);
            row.setPointsEarned(points);
            row.setMaxPoints((int) Math.round(question.getPoints() * repeatMultiplier));
            row.setUserAnswer(g.userAnswer);
            row.setCorrectAnswer(g.correctAnswer);
            row.setExplanation(question.getExplanation());
            attempt.addAnswer(row);
        }

        int questionCount = quiz.getQuestions().size();
        int score = (int) Math.round(100.0 * fractionSum / questionCount);
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        attempt.setQuestionCount(questionCount);
        attempt.setHintsUsed(hints);
        attempt.setBestStreak(bestStreak);
        attempt.setRepeatAttempt(repeat);
        attempt = attemptRepository.save(attempt);

        // ---- очки ----
        List<PointLineDto> lines = new ArrayList<>();
        lines.add(new PointLineDto("QUIZ_ANSWERS",
                repeat ? "Ответы на вопросы (повторное прохождение, очки × 0,5)" : "Ответы на вопросы", questionPoints));

        if (bestStreak >= 5) {
            lines.add(new PointLineDto("QUIZ_STREAK", "Серия из " + bestStreak + " верных ответов подряд",
                    rules.points(PointRules.STREAK_5)));
        } else if (bestStreak >= 3) {
            lines.add(new PointLineDto("QUIZ_STREAK", "Серия из " + bestStreak + " верных ответов подряд",
                    rules.points(PointRules.STREAK_3)));
        }

        long dailyLimit = Math.round(rules.value(PointRules.COMPLETE_DAILY_LIMIT));
        if (completedToday < dailyLimit) {
            lines.add(new PointLineDto("QUIZ_COMPLETE", "Завершение викторины", rules.points(PointRules.COMPLETE)));
        } else {
            lines.add(new PointLineDto("QUIZ_COMPLETE",
                    "Бонус за завершение уже получен " + dailyLimit + " раза сегодня", 0));
        }

        if (!repeat) {
            if (score >= 100) {
                lines.add(new PointLineDto("QUIZ_RESULT", "Результат 100%", rules.points(PointRules.RESULT_100)));
            } else if (score >= 80) {
                lines.add(new PointLineDto("QUIZ_RESULT", "Результат 80% и выше", rules.points(PointRules.RESULT_80)));
            }
            lines.add(new PointLineDto("QUIZ_FIRST_PASS", "Первое прохождение викторины", rules.points(PointRules.FIRST_PASS)));
        } else if (score >= 80) {
            lines.add(new PointLineDto("QUIZ_RESULT", "Бонус за результат даётся только за первое прохождение", 0));
        }

        int total = 0;
        for (PointLineDto line : lines) {
            pointService.award(user, line.code(), attempt.getAttemptId(), line.points());
            total += line.points();
        }
        attempt.setPointsEarned(total);

        return toResult(attempt, lines, user.getTotalPoints());
    }

    /** Подсказка: убирает часть неверных вариантов или показывает одну верную пару. */
    @Transactional(readOnly = true)
    public HintDto hint(Long quizId, Long questionId) {
        QuizQuestion question = findQuiz(quizId).getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Вопрос не найден"));
        List<AnswerOption> options = question.getOptions();
        String multiplier = "очки за вопрос уменьшатся вдвое";

        switch (question.getQuestionType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                List<Long> wrong = new ArrayList<>(options.stream()
                        .filter(o -> !o.isCorrect()).map(AnswerOption::getAnswerOptionId).toList());
                Collections.shuffle(wrong);
                // один ответ: остаются верный и один неверный; несколько ответов: убираем половину неверных
                int hide = question.getQuestionType() == QuestionType.SINGLE_CHOICE
                        ? Math.max(0, wrong.size() - 1)
                        : (wrong.size() + 1) / 2;
                if (hide == 0) {
                    return new HintDto(false, "Для этого вопроса подсказки нет", List.of(), List.of());
                }
                return new HintDto(true, "Убрали неверные варианты: " + hide + ". Учтите: " + multiplier + ".",
                        wrong.subList(0, hide), List.of());
            }
            case MATCHING -> {
                AnswerOption first = options.get(0);
                return new HintDto(true, "Показали одну верную пару. Учтите: " + multiplier + ".", List.of(),
                        List.of(new HintDto.Pair(first.getAnswerOptionId(), first.getMatchText())));
            }
            default -> {
                return new HintDto(false, "Для вопросов «верно / неверно» подсказки нет", List.of(), List.of());
            }
        }
    }

    // ---------- проверка одного вопроса ----------

    /** correct из total пунктов верно; тексты ответа пользователя и правильного ответа. */
    private record Graded(int correct, int total, String userAnswer, String correctAnswer) {
    }

    private Graded grade(QuizQuestion question, AttemptRequest.Answer answer) {
        List<AnswerOption> options = question.getOptions();
        if (question.getQuestionType() == QuestionType.MATCHING) {
            Map<Long, String> pairs = answer == null || answer.pairs() == null ? Map.of() : answer.pairs();
            int hits = 0;
            List<String> mine = new ArrayList<>();
            List<String> right = new ArrayList<>();
            for (AnswerOption o : options) {
                String chosen = pairs.get(o.getAnswerOptionId());
                if (chosen != null && chosen.trim().equals(Objects.toString(o.getMatchText(), "").trim())) {
                    hits++;
                }
                mine.add(o.getOptionText() + " → " + (chosen == null || chosen.isBlank() ? "—" : chosen));
                right.add(o.getOptionText() + " → " + o.getMatchText());
            }
            return new Graded(hits, options.size(), String.join("; ", mine), String.join("; ", right));
        }

        Set<Long> selected = new HashSet<>(answer == null || answer.optionIds() == null ? List.of() : answer.optionIds());
        Set<Long> correct = options.stream().filter(AnswerOption::isCorrect)
                .map(AnswerOption::getAnswerOptionId).collect(Collectors.toSet());
        String mine = options.stream().filter(o -> selected.contains(o.getAnswerOptionId()))
                .map(AnswerOption::getOptionText).collect(Collectors.joining("; "));
        String right = options.stream().filter(AnswerOption::isCorrect)
                .map(AnswerOption::getOptionText).collect(Collectors.joining("; "));
        if (mine.isEmpty()) mine = "—";

        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            // верные пункты = выбранные верные минус выбранные неверные (не меньше нуля)
            long hits = selected.stream().filter(correct::contains).count();
            long misses = selected.stream().filter(id -> !correct.contains(id)).count();
            int score = (int) Math.max(0, hits - misses);
            return new Graded(score, correct.size(), mine, right);
        }
        // один ответ и «верно / неверно»: верно, только если выбран ровно правильный вариант
        boolean ok = selected.size() == 1 && correct.containsAll(selected);
        return new Graded(ok ? 1 : 0, 1, mine, right);
    }

    // ---------- вспомогательное ----------

    private Quiz findQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Викторина не найдена"));
    }

    private static Instant startOfToday() {
        return LocalDate.now(BISHKEK).atStartOfDay(BISHKEK).toInstant();
    }

    /** Итог прохождения для ответа клиенту (используется и историей FR-5.4). */
    public static AttemptResultDto toResult(QuizAttempt attempt, List<PointLineDto> lines, int totalPoints) {
        List<QuestionResultDto> questions = attempt.getAnswers().stream()
                .map(a -> new QuestionResultDto(a.getQuestionId(), a.getQuestionText(), a.getQuestionType(),
                        a.isCorrect(), a.getCorrectItems(), a.getTotalItems(), a.isHintUsed(),
                        a.getPointsEarned(), a.getMaxPoints(), a.getUserAnswer(), a.getCorrectAnswer(),
                        a.getExplanation()))
                .toList();
        Quiz quiz = attempt.getQuiz();
        return new AttemptResultDto(attempt.getAttemptId(), quiz.getQuizId(), quiz.getTitle(), quiz.getTopic().name(),
                attempt.getScore(), attempt.getCorrectCount(), attempt.getQuestionCount(), attempt.getHintsUsed(),
                attempt.getBestStreak(), attempt.isRepeatAttempt(), attempt.getPointsEarned(), totalPoints,
                lines, questions, attempt.getCompletedAt());
    }
}

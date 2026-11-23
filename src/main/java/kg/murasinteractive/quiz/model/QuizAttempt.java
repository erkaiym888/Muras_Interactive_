package kg.murasinteractive.quiz.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Прохождение викторины (таблица quiz_attempt по ТЗ: attempt_id, user_id, quiz_id, score,
 * points_earned, completed_at) и дополнительные поля для истории и прогресса (FR-5.4).
 */
@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** Результат в процентах (0-100). */
    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    /** Самая длинная серия верных ответов подряд. */
    @Column(name = "best_streak", nullable = false)
    private int bestStreak;

    /** Повторное прохождение (очки за вопросы уменьшены, бонусы за результат не начислялись). */
    @Column(name = "repeat_attempt", nullable = false)
    private boolean repeatAttempt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuizAttemptAnswer> answers = new ArrayList<>();

    public QuizAttempt() {}

    public QuizAttempt(UserAccount user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
    }

    public void addAnswer(QuizAttemptAnswer answer) {
        answer.setAttempt(this);
        answers.add(answer);
    }

    public Long getAttemptId() { return attemptId; }
    public UserAccount getUser() { return user; }
    public Quiz getQuiz() { return quiz; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public boolean isRepeatAttempt() { return repeatAttempt; }
    public void setRepeatAttempt(boolean repeatAttempt) { this.repeatAttempt = repeatAttempt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<QuizAttemptAnswer> getAnswers() { return answers; }
}

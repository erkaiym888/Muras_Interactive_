package kg.murasinteractive.quiz.model;

import jakarta.persistence.*;

/**
 * Результат одного вопроса в прохождении. Тексты ответа пользователя и правильного ответа
 * сохраняются готовыми строками, чтобы история не менялась, если редактор потом изменит вопрос.
 */
@Entity
@Table(name = "quiz_attempt_answer")
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_answer_id")
    private Long attemptAnswerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    /** Вопрос может быть позже удалён редактором - тогда ссылка обнуляется, история остаётся. */
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    /** Верных пунктов из общего числа (для частично верных ответов). */
    @Column(name = "correct_items", nullable = false)
    private int correctItems;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "hint_used", nullable = false)
    private boolean hintUsed;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    public Long getAttemptAnswerId() { return attemptAnswerId; }
    public QuizAttempt getAttempt() { return attempt; }
    public void setAttempt(QuizAttempt attempt) { this.attempt = attempt; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public int getCorrectItems() { return correctItems; }
    public void setCorrectItems(int correctItems) { this.correctItems = correctItems; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public boolean isHintUsed() { return hintUsed; }
    public void setHintUsed(boolean hintUsed) { this.hintUsed = hintUsed; }
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public int getMaxPoints() { return maxPoints; }
    public void setMaxPoints(int maxPoints) { this.maxPoints = maxPoints; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}

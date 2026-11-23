package kg.murasinteractive.quiz.model;

import jakarta.persistence.*;

/**
 * Вариант ответа (таблица answer_option по ТЗ).
 * Для сопоставления option_text - левая часть пары, match_text - правильная правая часть.
 */
@Entity
@Table(name = "answer_option")
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_option_id")
    private Long answerOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    /** Правая часть пары (только для сопоставления). */
    @Column(name = "match_text", columnDefinition = "TEXT")
    private String matchText;

    /** Правильный ли это вариант. Клиенту НЕ отправляется. */
    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getAnswerOptionId() { return answerOptionId; }
    public QuizQuestion getQuestion() { return question; }
    public void setQuestion(QuizQuestion question) { this.question = question; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public String getMatchText() { return matchText; }
    public void setMatchText(String matchText) { this.matchText = matchText; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

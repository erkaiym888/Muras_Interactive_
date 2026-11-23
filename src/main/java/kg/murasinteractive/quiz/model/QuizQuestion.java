package kg.murasinteractive.quiz.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Вопрос викторины (таблица quiz_question по ТЗ). */
@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    /** Очки за вопрос. Как именно они начисляются, определяет проверка ответов (FR-5.2). */
    @Column(name = "points", nullable = false)
    private int points = 10;

    /** Пояснение, которое показывается после ответа (FR-5.2). */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<AnswerOption> options = new ArrayList<>();

    /** Обычный вариант ответа (один ответ, несколько ответов). */
    public QuizQuestion addOption(String text, boolean correct) {
        return add(text, null, correct);
    }

    /** Вопрос «верно / неверно»: два варианта, правильный зависит от утверждения. */
    public QuizQuestion addTrueFalse(boolean statementIsTrue) {
        add("Верно", null, statementIsTrue);
        return add("Неверно", null, !statementIsTrue);
    }

    /** Пара для сопоставления: слева left, справа right. */
    public QuizQuestion addPair(String left, String right) {
        return add(left, right, true);
    }

    private QuizQuestion add(String text, String matchText, boolean correct) {
        AnswerOption option = new AnswerOption();
        option.setQuestion(this);
        option.setOptionText(text);
        option.setMatchText(matchText);
        option.setCorrect(correct);
        option.setSortOrder(options.size() + 1);
        options.add(option);
        return this;
    }

    public Long getQuestionId() { return questionId; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<AnswerOption> getOptions() { return options; }
}

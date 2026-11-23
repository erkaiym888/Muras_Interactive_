package kg.murasinteractive.quiz.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Викторина (таблица quiz по ТЗ: quiz_id, title, topic, difficulty). */
@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 30)
    private QuizTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    public Quiz() {}

    public Quiz(String title, QuizTopic topic, Difficulty difficulty, String description) {
        this.title = title;
        this.topic = topic;
        this.difficulty = difficulty;
        this.description = description;
    }

    /** Добавляет вопрос в конец викторины и возвращает его для наполнения вариантами. */
    public QuizQuestion addQuestion(QuestionType type, String text, int points, String explanation) {
        QuizQuestion question = new QuizQuestion();
        question.setQuiz(this);
        question.setQuestionType(type);
        question.setQuestionText(text);
        question.setPoints(points);
        question.setExplanation(explanation);
        question.setSortOrder(questions.size() + 1);
        questions.add(question);
        return question;
    }

    public Long getQuizId() { return quizId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public QuizTopic getTopic() { return topic; }
    public void setTopic(QuizTopic topic) { this.topic = topic; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<QuizQuestion> getQuestions() { return questions; }
}

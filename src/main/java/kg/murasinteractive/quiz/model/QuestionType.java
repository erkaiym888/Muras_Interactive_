package kg.murasinteractive.quiz.model;

/** Четыре типа вопросов по FR-5.1. */
public enum QuestionType {
    /** Один правильный ответ. */
    SINGLE_CHOICE,
    /** Несколько правильных ответов. */
    MULTIPLE_CHOICE,
    /** Верно / неверно. */
    TRUE_FALSE,
    /** Сопоставление пар: option_text слева, match_text справа. */
    MATCHING
}

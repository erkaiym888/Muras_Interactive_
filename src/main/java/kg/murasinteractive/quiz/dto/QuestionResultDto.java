package kg.murasinteractive.quiz.dto;

/**
 * Результат одного вопроса: верно ли, сколько пунктов верно, очки,
 * ответ пользователя, правильный ответ и пояснение (FR-5.2).
 */
public record QuestionResultDto(Long questionId, String text, String type, boolean correct,
                                int correctItems, int totalItems, boolean hintUsed,
                                int pointsEarned, int maxPoints,
                                String userAnswer, String correctAnswer, String explanation) {
}

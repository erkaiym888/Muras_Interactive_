package kg.murasinteractive.quiz.dto;

import java.util.List;

/**
 * Вопрос для прохождения. Для сопоставления options - левые части пар,
 * matchChoices - перемешанные правые части. Правильные ответы сюда не попадают.
 */
public record QuizQuestionDto(Long id, String type, String text, int points,
                              List<QuizOptionDto> options, List<String> matchChoices) {
}

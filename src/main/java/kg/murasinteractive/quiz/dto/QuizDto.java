package kg.murasinteractive.quiz.dto;

import java.util.List;

public record QuizDto(Long id, String title, String topic, String difficulty,
                      String description, List<QuizQuestionDto> questions) {
}

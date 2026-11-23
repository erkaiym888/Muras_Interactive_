package kg.murasinteractive.quiz.dto;

public record QuizSummaryDto(Long id, String title, String topic, String difficulty,
                             String description, int questionCount) {
}

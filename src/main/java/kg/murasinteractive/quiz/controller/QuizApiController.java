package kg.murasinteractive.quiz.controller;

import kg.murasinteractive.quiz.dto.QuizDto;
import kg.murasinteractive.quiz.dto.QuizSummaryDto;
import kg.murasinteractive.quiz.service.QuizService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API викторин (FR-5.1). Доступно только вошедшим пользователям (см. SecurityConfig):
 * гость получает 401, и сайт предлагает ему войти или зарегистрироваться.
 *  GET /api/quizzes        - список викторин
 *  GET /api/quizzes/{id}   - вопросы викторины (без правильных ответов)
 * Проверка ответов и начисление очков - FR-5.2.
 */
@RestController
@RequestMapping("/api/quizzes")
public class QuizApiController {

    private final QuizService quizService;

    public QuizApiController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<QuizSummaryDto> quizzes() {
        return quizService.findQuizzes();
    }

    @GetMapping("/{quizId}")
    public QuizDto quiz(@PathVariable Long quizId) {
        return quizService.getQuiz(quizId);
    }
}

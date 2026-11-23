package kg.murasinteractive.quiz.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.quiz.dto.AttemptRequest;
import kg.murasinteractive.quiz.dto.AttemptResultDto;
import kg.murasinteractive.quiz.dto.HintDto;
import kg.murasinteractive.quiz.service.QuizScoringService;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 *  GET  /api/quizzes/{id}/questions/{qid}/hint - подсказка (очки за вопрос × 0,5)        - FR-5.2
 *  POST /api/quizzes/{id}/attempts               - проверить ответы, начислить очки       - FR-5.2
 */
@RestController
@RequestMapping("/api/quizzes")
public class QuizApiController {

    private final QuizService quizService;
    private final QuizScoringService scoringService;
    private final CurrentUserService currentUser;

    public QuizApiController(QuizService quizService, QuizScoringService scoringService,
                             CurrentUserService currentUser) {
        this.quizService = quizService;
        this.scoringService = scoringService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<QuizSummaryDto> quizzes() {
        return quizService.findQuizzes();
    }

    @GetMapping("/{quizId}")
    public QuizDto quiz(@PathVariable Long quizId) {
        return quizService.getQuiz(quizId);
    }

    @GetMapping("/{quizId}/questions/{questionId}/hint")
    public HintDto hint(@PathVariable Long quizId, @PathVariable Long questionId) {
        return scoringService.hint(quizId, questionId);
    }

    /** Проверка ответов и начисление очков (FR-5.2). */
    @PostMapping("/{quizId}/attempts")
    public AttemptResultDto submit(@PathVariable Long quizId, @Valid @RequestBody AttemptRequest request,
                                   Authentication authentication) {
        Long userId = currentUser.require(authentication).getUserId();
        return scoringService.submit(userId, quizId, request);
    }
}

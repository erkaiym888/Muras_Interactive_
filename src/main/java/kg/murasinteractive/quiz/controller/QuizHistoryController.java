package kg.murasinteractive.quiz.controller;

import kg.murasinteractive.quiz.dto.AttemptResultDto;
import kg.murasinteractive.quiz.dto.HistoryItemDto;
import kg.murasinteractive.quiz.dto.ProgressDto;
import kg.murasinteractive.quiz.service.QuizHistoryService;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * История прохождений и прогресс (FR-5.4). Только для вошедших (SecurityConfig: /api/quizzes/**),
 * каждый видит только свои данные.
 *  GET /api/quizzes/history               - все прохождения, новые сверху (?quizId= - по одной викторине)
 *  GET /api/quizzes/history/{attemptId}   - разбор прохождения: ответы, пояснения, очки
 *  GET /api/quizzes/progress              - прогресс: по темам и по каждой викторине
 */
@RestController
@RequestMapping("/api/quizzes")
public class QuizHistoryController {

    private final QuizHistoryService historyService;
    private final CurrentUserService currentUser;

    public QuizHistoryController(QuizHistoryService historyService, CurrentUserService currentUser) {
        this.historyService = historyService;
        this.currentUser = currentUser;
    }

    @GetMapping("/history")
    public List<HistoryItemDto> history(@RequestParam(required = false) Long quizId, Authentication authentication) {
        return historyService.history(currentUser.require(authentication).getUserId(), quizId);
    }

    @GetMapping("/history/{attemptId}")
    public AttemptResultDto attempt(@PathVariable Long attemptId, Authentication authentication) {
        UserAccount user = currentUser.require(authentication);
        return historyService.attempt(user, attemptId);
    }

    @GetMapping("/progress")
    public ProgressDto progress(Authentication authentication) {
        return historyService.progress(currentUser.require(authentication));
    }
}

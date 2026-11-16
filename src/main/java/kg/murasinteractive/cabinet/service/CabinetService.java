package kg.murasinteractive.cabinet.service;

import kg.murasinteractive.cabinet.dto.CabinetAwardDto;
import kg.murasinteractive.cabinet.dto.CabinetDto;
import kg.murasinteractive.cabinet.dto.CabinetHistoryItemDto;
import kg.murasinteractive.cabinet.dto.CabinetProfileDto;
import kg.murasinteractive.favorite.service.FavoriteService;
import kg.murasinteractive.quiz.service.QuizHistoryService;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabinetService {

    /** Сколько последних прохождений показывать в кабинете; полная история - на странице викторин. */
    private static final int HISTORY_LIMIT = 10;

    private final FavoriteService favoriteService;
    private final QuizHistoryService historyService;

    public CabinetService(FavoriteService favoriteService, QuizHistoryService historyService) {
        this.favoriteService = favoriteService;
        this.historyService = historyService;
    }

    public CabinetDto build(UserAccount user) {
        CabinetProfileDto profile = new CabinetProfileDto(user.getDisplayName(), user.getEmail(),
                user.getRole().getRoleName(), user.getRegisteredAt(), user.getTotalPoints());
        // FR-5.4: последние прохождения викторин из таблицы quiz_attempt
        List<CabinetHistoryItemDto> history = historyService.history(user.getUserId(), null).stream()
                .limit(HISTORY_LIMIT)
                .map(h -> new CabinetHistoryItemDto(h.attemptId(), h.quizId(), h.quizTitle(), h.score(),
                        h.pointsEarned(), h.repeatAttempt(), h.completedAt()))
                .toList();
        // TODO FR-5.3: награды (значки и уровни)
        List<CabinetAwardDto> awards = List.of();
        return new CabinetDto(profile, history, awards, favoriteService.list(user));
    }
}

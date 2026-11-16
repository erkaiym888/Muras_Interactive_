package kg.murasinteractive.cabinet.service;

import kg.murasinteractive.cabinet.dto.CabinetAwardDto;
import kg.murasinteractive.cabinet.dto.CabinetDto;
import kg.murasinteractive.cabinet.dto.CabinetHistoryItemDto;
import kg.murasinteractive.cabinet.dto.CabinetProfileDto;
import kg.murasinteractive.favorite.service.FavoriteService;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabinetService {

    private final FavoriteService favoriteService;

    public CabinetService(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    public CabinetDto build(UserAccount user) {
        CabinetProfileDto profile = new CabinetProfileDto(user.getDisplayName(), user.getEmail(),
                user.getRole().getRoleName(), user.getRegisteredAt(), user.getTotalPoints());
        // TODO FR-5.4: история прохождений викторин берётся из таблицы quiz_attempt
        List<CabinetHistoryItemDto> history = List.of();
        // TODO FR-5.3: награды (значки и уровни)
        List<CabinetAwardDto> awards = List.of();
        return new CabinetDto(profile, history, awards, favoriteService.list(user));
    }
}

package kg.murasinteractive.cabinet.dto;

import kg.murasinteractive.favorite.dto.FavoriteDto;

import java.util.List;

/** Всё, что показывает личный кабинет (FR-6.2): профиль, очки, награды, история, избранное. */
public record CabinetDto(CabinetProfileDto profile, List<CabinetHistoryItemDto> history,
                         List<CabinetAwardDto> awards, List<FavoriteDto> favorites) {
}

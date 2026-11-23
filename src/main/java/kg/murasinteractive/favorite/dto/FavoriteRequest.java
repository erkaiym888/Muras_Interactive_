package kg.murasinteractive.favorite.dto;

import jakarta.validation.constraints.NotNull;
import kg.murasinteractive.favorite.model.FavoriteType;

public record FavoriteRequest(@NotNull(message = "Не указан тип материала") FavoriteType itemType,
                              @NotNull(message = "Не указан материал") Long itemId) {
}

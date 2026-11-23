package kg.murasinteractive.favorite.dto;

/** Строка списка избранного: что это, как называется и к какой категории или типу относится. */
public record FavoriteDto(String itemType, Long itemId, String title, String subtitle) {
}

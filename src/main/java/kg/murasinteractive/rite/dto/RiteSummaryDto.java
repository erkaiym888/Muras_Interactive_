package kg.murasinteractive.rite.dto;

/** Краткие данные для карточки в каталоге. */
public record RiteSummaryDto(Long id, String title, String category, String summary, String imageUrl) {
}

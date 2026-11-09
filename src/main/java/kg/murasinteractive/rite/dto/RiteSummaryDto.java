package kg.murasinteractive.rite.dto;

/** Краткие данные для карточки в каталоге. categoryId нужен фильтру (FR-2.3). */
public record RiteSummaryDto(Long id, String title, Long categoryId, String category, String summary,
                             String imageUrl) {
}

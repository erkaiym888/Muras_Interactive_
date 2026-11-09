package kg.murasinteractive.folklore.dto;

import java.util.List;

/**
 * Карточка героя или произведения (FR-3.2): имя/название, тип, краткое описание,
 * связанные карточки и иллюстрации. Для героя related - его произведения, для произведения - его герои.
 */
public record FolkloreDetailDto(String kind, Long id, String title, String type, String description,
                                List<FolkloreLinkDto> related, List<FolkloreImageDto> images) {
}

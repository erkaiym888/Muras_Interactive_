package kg.murasinteractive.folklore.dto;

/** Строка каталога: герой (kind = "hero") или произведение (kind = "work"). */
public record FolkloreItemDto(String kind, Long id, String title, String type, String summary, String imageUrl) {
}

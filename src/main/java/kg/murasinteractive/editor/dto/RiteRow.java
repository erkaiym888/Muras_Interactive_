package kg.murasinteractive.editor.dto;

/** Строка списка материалов в редакторе: обряд в любом статусе. */
public record RiteRow(Long id, String title, Long categoryId, String category, String status,
                      int stepCount, String imageUrl) {
}

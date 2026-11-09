package kg.murasinteractive.editor.dto;

/**
 * Строка списка фольклора в редакторе.
 * kind: hero - герой, work - произведение; linkCount - число связанных произведений или героев.
 */
public record FolkloreRow(String kind, Long id, String title, Long typeId, String type, int linkCount,
                          String imageUrl) {
}

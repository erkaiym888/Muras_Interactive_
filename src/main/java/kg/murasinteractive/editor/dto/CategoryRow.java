package kg.murasinteractive.editor.dto;

/** Категория в редакторе: сколько обрядов к ней привязано (удалить можно только пустую). */
public record CategoryRow(Long id, String name, long riteCount) {
}

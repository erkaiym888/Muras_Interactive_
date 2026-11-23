package kg.murasinteractive.content;

import java.util.Map;

/**
 * Статусы материала (FR-7.2) - общие для обрядов, героев и произведений.
 * DRAFT - черновик, виден только в редакторе; PUBLISHED - опубликован, виден всем;
 * HIDDEN - скрыт: убран с сайта, но не удалён.
 */
public final class ContentStatus {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String HIDDEN = "HIDDEN";

    /** Для проверки в DTO: @Pattern(regexp = ContentStatus.PATTERN). */
    public static final String PATTERN = "DRAFT|PUBLISHED|HIDDEN";

    public static final Map<String, String> LABELS = Map.of(
            DRAFT, "Черновик", PUBLISHED, "Опубликован", HIDDEN, "Скрыт");

    private ContentStatus() {
    }
}

package kg.murasinteractive.search.dto;

/**
 * Одна находка поиска (FR-4.1).
 *
 * @param kind    rite, hero, work или yurt
 * @param id      идентификатор (у элемента юрты - null)
 * @param key     ключ элемента юрты на схеме (у остальных - null)
 * @param snippet фрагмент текста; найденные слова обрамлены [[ и ]] - фронтенд подсвечивает их
 */
public record SearchResultDto(String kind, Long id, String key, String title, String subtitle, String snippet) {
}

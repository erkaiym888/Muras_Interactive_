package kg.murasinteractive.relation.dto;

/**
 * Один связанный материал (FR-3.3).
 *
 * @param kind     rite, hero, work или yurt
 * @param id       идентификатор (для элемента юрты - null, используется key)
 * @param key      ключ элемента юрты на схеме (tunduk, kerege...), для остальных - null
 * @param subtitle категория обряда, тип фольклора или группа элемента юрты
 * @param via      если связь косвенная - через какой материал (например, произведение «Манас»); прямая - null
 */
public record RelationNodeDto(String kind, Long id, String key, String title, String subtitle, String via) {
}

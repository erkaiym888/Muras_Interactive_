package kg.murasinteractive.i18n.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Какие материалы и поля переводятся (FR-8.1): значения entity_type и field_name в content_translation.
 * Этапы обряда хранятся при самом обряде как step.{номер}.title / step.{номер}.text - так перевод
 * не теряется, когда редактор пересохраняет этапы.
 */
public final class Translatable {

    public static final String RITE = "RITE";
    public static final String RITE_CATEGORY = "RITE_CATEGORY";
    public static final String FOLKLORE_HERO = "FOLKLORE_HERO";
    public static final String FOLKLORE_WORK = "FOLKLORE_WORK";
    public static final String FOLKLORE_TYPE = "FOLKLORE_TYPE";

    /** Поля каждого вида материала с подписями для формы перевода. */
    public static final Map<String, Map<String, String>> FIELDS = new LinkedHashMap<>();
    public static final Map<String, String> TITLES = new LinkedHashMap<>();

    static {
        TITLES.put(RITE, "Обряды");
        TITLES.put(RITE_CATEGORY, "Категории обрядов");
        TITLES.put(FOLKLORE_HERO, "Герои фольклора");
        TITLES.put(FOLKLORE_WORK, "Произведения");
        TITLES.put(FOLKLORE_TYPE, "Типы фольклора");

        FIELDS.put(RITE, ordered("title", "Название", "summary", "Аннотация", "description", "Описание",
                "symbolism", "Символика", "sources", "Источники"));
        FIELDS.put(RITE_CATEGORY, ordered("name", "Название"));
        FIELDS.put(FOLKLORE_HERO, ordered("name", "Имя", "description", "Описание"));
        FIELDS.put(FOLKLORE_WORK, ordered("title", "Название", "summary", "Краткое содержание"));
        FIELDS.put(FOLKLORE_TYPE, ordered("name", "Название"));
    }

    public static String stepTitle(int order) { return "step." + order + ".title"; }
    public static String stepText(int order) { return "step." + order + ".text"; }

    private static Map<String, String> ordered(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    private Translatable() {}
}

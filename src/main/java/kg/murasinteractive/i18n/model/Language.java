package kg.murasinteractive.i18n.model;

import jakarta.persistence.*;

/**
 * Язык сайта (FR-8.1), справочник language. Новый язык - это новая строка (или включение существующей),
 * а не изменение кода: тексты контента лежат в content_translation, тексты интерфейса - в /locales/{code}.json.
 */
@Entity
@Table(name = "language")
public class Language {

    @Id
    @Column(name = "language_code", length = 5)
    private String code;

    /** Название по-русски - для панели администратора. */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** Самоназвание - для переключателя языка: «Кыргызча», «English». */
    @Column(name = "native_name", nullable = false, length = 50)
    private String nativeName;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    /** Язык, на котором хранятся основные поля материалов (русский). Он один. */
    @Column(name = "is_default", nullable = false)
    private boolean defaultLanguage;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Language() {}

    public Language(String code, String name, String nativeName, boolean enabled, boolean defaultLanguage, int sortOrder) {
        this.code = code;
        this.name = name;
        this.nativeName = nativeName;
        this.enabled = enabled;
        this.defaultLanguage = defaultLanguage;
        this.sortOrder = sortOrder;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNativeName() { return nativeName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDefaultLanguage() { return defaultLanguage; }
    public int getSortOrder() { return sortOrder; }
}

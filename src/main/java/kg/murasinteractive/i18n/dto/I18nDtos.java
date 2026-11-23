package kg.murasinteractive.i18n.dto;

import java.util.List;
import java.util.Map;

/** DTO многоязычности (FR-8.1) собраны в одном файле: они маленькие и используются вместе. */
public final class I18nDtos {

    /** Язык для переключателя на сайте. */
    public record LanguageDto(String code, String nativeName) {}

    /** Ответ GET /api/languages: включённые языки и язык по умолчанию. */
    public record LanguagesDto(String defaultCode, List<LanguageDto> languages) {}

    /** Язык в панели администратора. */
    public record AdminLanguageDto(String code, String name, String nativeName, boolean enabled,
                                   boolean defaultLanguage, long translationCount) {}

    public record LanguageToggleRequest(Boolean enabled) {}

    /** Вид материала для формы перевода. */
    public record KindDto(String type, String title) {}

    /** Что можно переводить и на какие языки (все, кроме основного, включая ещё выключенные). */
    public record TranslationMetaDto(List<KindDto> kinds, List<AdminLanguageDto> languages) {}

    /** Строка списка материалов: сколько полей уже переведено. */
    public record TranslationItemDto(Long id, String title, int filled, int total) {}

    /** Одно поле: русский оригинал и перевод (null - перевода нет). */
    public record TranslationFieldDto(String field, String label, String original, String translation, boolean multiline) {}

    public record TranslationFormDto(String type, Long id, String title, String language, List<TranslationFieldDto> fields) {}

    /** Сохранение: поле -> текст; пустой текст удаляет перевод. */
    public record TranslationSaveRequest(Map<String, String> fields) {}

    private I18nDtos() {}
}

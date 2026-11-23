package kg.murasinteractive.i18n.service;

import kg.murasinteractive.i18n.model.ContentTranslation;
import kg.murasinteractive.i18n.repository.ContentTranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Подставляет переводы в публичные ответы (FR-8.1).
 * Для языка по умолчанию ничего не загружает; если перевода поля нет - остаётся русский текст.
 * Переводы загружаются одним запросом на вид материала, а не по одному полю.
 */
@Service
public class Translator {

    private final ContentTranslationRepository translationRepository;
    private final LanguageService languageService;

    public Translator(ContentTranslationRepository translationRepository, LanguageService languageService) {
        this.translationRepository = translationRepository;
        this.languageService = languageService;
    }

    /** Переводы на язык текущего запроса. */
    @Transactional(readOnly = true)
    public Texts load(String entityType, Collection<Long> ids) {
        return load(languageService.current(), entityType, ids);
    }

    @Transactional(readOnly = true)
    public Texts load(String languageCode, String entityType, Collection<Long> ids) {
        if (ids.isEmpty() || languageCode.equals(languageService.defaultCode())) return Texts.EMPTY;
        Map<Long, Map<String, String>> map = new HashMap<>();
        for (ContentTranslation t : translationRepository.findByEntityTypeAndLanguageCodeAndEntityIdIn(
                entityType, languageCode, new HashSet<>(ids))) {
            map.computeIfAbsent(t.getEntityId(), k -> new HashMap<>()).put(t.getFieldName(), t.getTextValue());
        }
        return new Texts(map);
    }

    public String currentLanguage() {
        return languageService.current();
    }

    /** Загруженные переводы одного вида материала. */
    public static final class Texts {
        static final Texts EMPTY = new Texts(Map.of());
        private final Map<Long, Map<String, String>> values;

        Texts(Map<Long, Map<String, String>> values) { this.values = values; }

        /** Перевод поля или исходный текст, если перевода нет. */
        public String get(Long id, String field, String fallback) {
            String text = values.getOrDefault(id, Map.of()).get(field);
            return text == null || text.isBlank() ? fallback : text;
        }
    }
}

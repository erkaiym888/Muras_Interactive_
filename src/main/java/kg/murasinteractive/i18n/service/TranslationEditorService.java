package kg.murasinteractive.i18n.service;

import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreType;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreTypeRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.i18n.dto.I18nDtos.*;
import kg.murasinteractive.i18n.model.ContentTranslation;
import kg.murasinteractive.i18n.model.Language;
import kg.murasinteractive.i18n.repository.ContentTranslationRepository;
import kg.murasinteractive.i18n.repository.LanguageRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.model.RiteCategory;
import kg.murasinteractive.rite.model.RiteStep;
import kg.murasinteractive.rite.repository.RiteCategoryRepository;
import kg.murasinteractive.rite.repository.RiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Перевод материалов редактором (FR-8.1): русский оригинал рядом с полем перевода.
 * Набор полей берётся из Translatable, поэтому новый язык не требует новых форм или кода.
 */
@Service
public class TranslationEditorService {

    /** Оригинал одного поля. */
    private record Source(String label, String text, boolean multiline) {}

    private final ContentTranslationRepository translationRepository;
    private final LanguageRepository languageRepository;
    private final RiteRepository riteRepository;
    private final RiteCategoryRepository categoryRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;
    private final FolkloreTypeRepository typeRepository;

    public TranslationEditorService(ContentTranslationRepository translationRepository, LanguageRepository languageRepository,
                                    RiteRepository riteRepository, RiteCategoryRepository categoryRepository,
                                    FolkloreHeroRepository heroRepository, FolkloreWorkRepository workRepository,
                                    FolkloreTypeRepository typeRepository) {
        this.translationRepository = translationRepository;
        this.languageRepository = languageRepository;
        this.riteRepository = riteRepository;
        this.categoryRepository = categoryRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
        this.typeRepository = typeRepository;
    }

    @Transactional(readOnly = true)
    public TranslationMetaDto meta() {
        List<KindDto> kinds = Translatable.TITLES.entrySet().stream()
                .map(e -> new KindDto(e.getKey(), e.getValue())).toList();
        List<AdminLanguageDto> languages = languageRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(l -> !l.isDefaultLanguage())
                .map(l -> new AdminLanguageDto(l.getCode(), l.getName(), l.getNativeName(), l.isEnabled(), false,
                        translationRepository.countByLanguageCode(l.getCode())))
                .toList();
        return new TranslationMetaDto(kinds, languages);
    }

    /** Материалы одного вида и сколько полей у каждого уже переведено. */
    @Transactional(readOnly = true)
    public List<TranslationItemDto> items(String type, String lang) {
        checkLanguage(lang);
        Map<Long, Set<String>> filled = new HashMap<>();
        for (ContentTranslation t : translationRepository.findByEntityTypeAndLanguageCode(checkType(type), lang)) {
            filled.computeIfAbsent(t.getEntityId(), k -> new HashSet<>()).add(t.getFieldName());
        }
        List<TranslationItemDto> result = new ArrayList<>();
        for (Map.Entry<Long, String> item : titles(type).entrySet()) {
            Map<String, Source> sources = sources(type, item.getKey());
            Set<String> done = filled.getOrDefault(item.getKey(), Set.of());
            int count = (int) sources.keySet().stream().filter(done::contains).count();
            result.add(new TranslationItemDto(item.getKey(), item.getValue(), count, sources.size()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public TranslationFormDto form(String type, Long id, String lang) {
        checkLanguage(lang);
        Map<String, Source> sources = sources(checkType(type), id);
        Map<String, String> existing = new HashMap<>();
        translationRepository.findByEntityTypeAndLanguageCodeAndEntityIdIn(type, lang, List.of(id))
                .forEach(t -> existing.put(t.getFieldName(), t.getTextValue()));
        List<TranslationFieldDto> fields = sources.entrySet().stream()
                .map(e -> new TranslationFieldDto(e.getKey(), e.getValue().label(), e.getValue().text(),
                        existing.get(e.getKey()), e.getValue().multiline()))
                .toList();
        return new TranslationFormDto(type, id, titles(type).get(id), lang, fields);
    }

    @Transactional
    public TranslationFormDto save(String type, Long id, String lang, Map<String, String> fields) {
        checkLanguage(lang);
        Map<String, Source> sources = sources(checkType(type), id);
        for (Map.Entry<String, String> e : (fields == null ? Map.<String, String>of() : fields).entrySet()) {
            if (!sources.containsKey(e.getKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У материала нет поля «" + e.getKey() + "»");
            }
            String text = e.getValue() == null ? "" : e.getValue().trim();
            Optional<ContentTranslation> current = translationRepository
                    .findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(type, id, lang, e.getKey());
            if (text.isEmpty()) {
                current.ifPresent(translationRepository::delete);
            } else if (current.isPresent()) {
                current.get().setTextValue(text);
            } else {
                translationRepository.save(new ContentTranslation(type, id, lang, e.getKey(), text));
            }
        }
        translationRepository.flush();
        return form(type, id, lang);
    }

    // ---------- Оригиналы по видам материалов ----------

    private Map<Long, String> titles(String type) {
        Map<Long, String> map = new LinkedHashMap<>();
        switch (type) {
            case Translatable.RITE -> riteRepository.findAllByOrderByRiteIdDesc().forEach(r -> map.put(r.getRiteId(), r.getTitle()));
            case Translatable.RITE_CATEGORY -> categoryRepository.findAllByOrderByRiteCategoryIdAsc()
                    .forEach(c -> map.put(c.getRiteCategoryId(), c.getCategoryName()));
            case Translatable.FOLKLORE_HERO -> heroRepository.findAllByOrderByHeroIdAsc().forEach(h -> map.put(h.getHeroId(), h.getName()));
            case Translatable.FOLKLORE_WORK -> workRepository.findAllByOrderByWorkIdAsc().forEach(w -> map.put(w.getWorkId(), w.getTitle()));
            case Translatable.FOLKLORE_TYPE -> typeRepository.findAllByOrderByFolkloreTypeIdAsc()
                    .forEach(t -> map.put(t.getFolkloreTypeId(), t.getTypeName()));
            default -> throw unknownType(type);
        }
        return map;
    }

    private Map<String, Source> sources(String type, Long id) {
        Map<String, String> labels = Translatable.FIELDS.get(type);
        Map<String, Source> map = new LinkedHashMap<>();
        switch (type) {
            case Translatable.RITE -> {
                Rite r = riteRepository.findById(id).orElseThrow(() -> notFound(type));
                put(map, labels, "title", r.getTitle(), false);
                put(map, labels, "summary", r.getSummary(), true);
                put(map, labels, "description", r.getDescription(), true);
                put(map, labels, "symbolism", r.getSymbolism(), true);
                put(map, labels, "sources", r.getSources(), true);
                for (RiteStep s : r.getSteps()) {
                    String n = "Этап " + s.getStepOrder();
                    if (s.getStepTitle() != null && !s.getStepTitle().isBlank()) {
                        map.put(Translatable.stepTitle(s.getStepOrder()), new Source(n + " — заголовок", s.getStepTitle(), false));
                    }
                    map.put(Translatable.stepText(s.getStepOrder()), new Source(n + " — текст", s.getStepText(), true));
                }
            }
            case Translatable.RITE_CATEGORY -> {
                RiteCategory c = categoryRepository.findById(id).orElseThrow(() -> notFound(type));
                put(map, labels, "name", c.getCategoryName(), false);
            }
            case Translatable.FOLKLORE_HERO -> {
                FolkloreHero h = heroRepository.findById(id).orElseThrow(() -> notFound(type));
                put(map, labels, "name", h.getName(), false);
                put(map, labels, "description", h.getDescription(), true);
            }
            case Translatable.FOLKLORE_WORK -> {
                FolkloreWork w = workRepository.findById(id).orElseThrow(() -> notFound(type));
                put(map, labels, "title", w.getTitle(), false);
                put(map, labels, "summary", w.getSummary(), true);
            }
            case Translatable.FOLKLORE_TYPE -> {
                FolkloreType t = typeRepository.findById(id).orElseThrow(() -> notFound(type));
                put(map, labels, "name", t.getTypeName(), false);
            }
            default -> throw unknownType(type);
        }
        return map;
    }

    /** Пустые поля оригинала не переводим - переводить нечего. */
    private static void put(Map<String, Source> map, Map<String, String> labels, String field, String text, boolean multiline) {
        if (text != null && !text.isBlank()) map.put(field, new Source(labels.get(field), text, multiline));
    }

    private String checkType(String type) {
        if (!Translatable.FIELDS.containsKey(type)) throw unknownType(type);
        return type;
    }

    private void checkLanguage(String lang) {
        Language language = languageRepository.findById(lang == null ? "" : lang)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный язык: " + lang));
        if (language.isDefaultLanguage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Основной язык редактируется в самих материалах, а не в переводах");
        }
    }

    private static ResponseStatusException unknownType(String type) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный вид материала: " + type);
    }

    private static ResponseStatusException notFound(String type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден");
    }
}

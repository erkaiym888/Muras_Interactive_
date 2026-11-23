package kg.murasinteractive.rite.service;

import kg.murasinteractive.i18n.service.Translatable;
import kg.murasinteractive.i18n.service.Translator;
import kg.murasinteractive.rite.dto.*;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.model.RiteCategory;
import kg.murasinteractive.rite.repository.RiteCategoryRepository;
import kg.murasinteractive.rite.repository.RiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RiteService {

    private final RiteRepository riteRepository;
    private final RiteCategoryRepository categoryRepository;
    private final Translator translator;

    public RiteService(RiteRepository riteRepository, RiteCategoryRepository categoryRepository, Translator translator) {
        this.riteRepository = riteRepository;
        this.categoryRepository = categoryRepository;
        this.translator = translator;
    }

    @Transactional(readOnly = true)
    public List<RiteCategoryDto> findCategories() {
        List<RiteCategory> categories = categoryRepository.findAllByOrderByRiteCategoryIdAsc();
        Translator.Texts names = translator.load(Translatable.RITE_CATEGORY,
                categories.stream().map(RiteCategory::getRiteCategoryId).toList());
        return categories.stream()
                .map(c -> new RiteCategoryDto(c.getRiteCategoryId(),
                        names.get(c.getRiteCategoryId(), "name", c.getCategoryName())))
                .toList();
    }

    /** Варианты сортировки каталога (FR-2.3). */
    public static final String SORT_TITLE = "title";
    public static final String SORT_TITLE_DESC = "title_desc";
    public static final String SORT_NEW = "new";
    public static final String SORT_OLD = "old";

    /**
     * Опубликованные обряды для каталога с фильтром и сортировкой (FR-2.3).
     *
     * @param categoryId только эта категория; null - все
     * @param query      поиск по названию и аннотации на языке запроса (FR-8.1) без учёта регистра; пусто - без поиска
     * @param sort       title (А-Я), title_desc (Я-А), new (сначала новые), old (сначала давние)
     */
    @Transactional(readOnly = true)
    public List<RiteSummaryDto> findPublishedSummaries(Long categoryId, String query, String sort) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Collator collator = Collator.getInstance(Locale.forLanguageTag(translator.currentLanguage()));
        Comparator<RiteSummaryDto> byTitle = Comparator.comparing(RiteSummaryDto::title, collator);
        Comparator<RiteSummaryDto> byId = Comparator.comparing(RiteSummaryDto::id);
        Comparator<RiteSummaryDto> order = switch (sort == null || sort.isBlank() ? SORT_TITLE : sort) {
            case SORT_TITLE -> byTitle;
            case SORT_TITLE_DESC -> byTitle.reversed();
            case SORT_NEW -> byId.reversed();
            case SORT_OLD -> byId;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Неизвестная сортировка: " + sort);
        };
        List<Rite> rites = riteRepository.findByStatusOrderByRiteIdAsc(Rite.STATUS_PUBLISHED).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getRiteCategory().getRiteCategoryId()))
                .toList();
        Translator.Texts texts = translator.load(Translatable.RITE, rites.stream().map(Rite::getRiteId).toList());
        Translator.Texts categories = translator.load(Translatable.RITE_CATEGORY,
                rites.stream().map(r -> r.getRiteCategory().getRiteCategoryId()).distinct().toList());
        // Ищем и сортируем уже по переведённым текстам - так, как их видит посетитель
        return rites.stream()
                .map(r -> toSummary(r, texts, categories))
                .filter(d -> needle.isEmpty() || contains(d.title(), needle) || contains(d.summary(), needle))
                .sorted(order)
                .toList();
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** Полная карточка одного опубликованного обряда; если нет - ошибка 404. */
    @Transactional(readOnly = true)
    public RiteDetailDto getPublishedDetail(Long riteId) {
        Rite rite = riteRepository.findByRiteIdAndStatus(riteId, Rite.STATUS_PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обряд не найден"));
        return toDetail(rite, translator.load(Translatable.RITE, List.of(riteId)),
                translator.load(Translatable.RITE_CATEGORY, List.of(rite.getRiteCategory().getRiteCategoryId())));
    }

    private RiteSummaryDto toSummary(Rite r, Translator.Texts t, Translator.Texts c) {
        String imageUrl = r.getMedia().isEmpty() ? null : r.getMedia().get(0).getFilePath();
        Long id = r.getRiteId();
        RiteCategory category = r.getRiteCategory();
        return new RiteSummaryDto(id, t.get(id, "title", r.getTitle()), category.getRiteCategoryId(),
                c.get(category.getRiteCategoryId(), "name", category.getCategoryName()),
                t.get(id, "summary", r.getSummary()), imageUrl);
    }

    private RiteDetailDto toDetail(Rite r, Translator.Texts t, Translator.Texts c) {
        Long id = r.getRiteId();
        List<RiteStepDto> steps = r.getSteps().stream()
                .map(s -> new RiteStepDto(s.getStepOrder(),
                        t.get(id, Translatable.stepTitle(s.getStepOrder()), s.getStepTitle()),
                        t.get(id, Translatable.stepText(s.getStepOrder()), s.getStepText())))
                .toList();
        List<RiteImageDto> images = r.getMedia().stream()
                .map(m -> new RiteImageDto(m.getFilePath(), m.getCaption()))
                .toList();
        RiteCategory category = r.getRiteCategory();
        return new RiteDetailDto(id, t.get(id, "title", r.getTitle()),
                c.get(category.getRiteCategoryId(), "name", category.getCategoryName()),
                t.get(id, "summary", r.getSummary()), t.get(id, "description", r.getDescription()),
                t.get(id, "symbolism", r.getSymbolism()), t.get(id, "sources", r.getSources()), steps, images);
    }
}

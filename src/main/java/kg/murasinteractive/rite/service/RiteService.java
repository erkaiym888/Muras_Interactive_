package kg.murasinteractive.rite.service;

import kg.murasinteractive.rite.dto.*;
import kg.murasinteractive.rite.model.Rite;
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

    public RiteService(RiteRepository riteRepository, RiteCategoryRepository categoryRepository) {
        this.riteRepository = riteRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<RiteCategoryDto> findCategories() {
        return categoryRepository.findAllByOrderByRiteCategoryIdAsc().stream()
                .map(c -> new RiteCategoryDto(c.getRiteCategoryId(), c.getCategoryName()))
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
     * @param query      поиск по названию и аннотации без учёта регистра; пусто - без поиска
     * @param sort       title (А-Я), title_desc (Я-А), new (сначала новые), old (сначала давние)
     */
    @Transactional(readOnly = true)
    public List<RiteSummaryDto> findPublishedSummaries(Long categoryId, String query, String sort) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Collator collator = Collator.getInstance(Locale.forLanguageTag("ru"));
        Comparator<Rite> byTitle = Comparator.comparing(Rite::getTitle, collator);
        Comparator<Rite> byId = Comparator.comparing(Rite::getRiteId);
        Comparator<Rite> order = switch (sort == null || sort.isBlank() ? SORT_TITLE : sort) {
            case SORT_TITLE -> byTitle;
            case SORT_TITLE_DESC -> byTitle.reversed();
            case SORT_NEW -> byId.reversed();
            case SORT_OLD -> byId;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Неизвестная сортировка: " + sort);
        };
        return riteRepository.findByStatusOrderByRiteIdAsc(Rite.STATUS_PUBLISHED).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getRiteCategory().getRiteCategoryId()))
                .filter(r -> needle.isEmpty() || contains(r.getTitle(), needle) || contains(r.getSummary(), needle))
                .sorted(order)
                .map(this::toSummary)
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
        return toDetail(rite);
    }

    private RiteSummaryDto toSummary(Rite r) {
        String imageUrl = r.getMedia().isEmpty() ? null : r.getMedia().get(0).getFilePath();
        return new RiteSummaryDto(r.getRiteId(), r.getTitle(), r.getRiteCategory().getRiteCategoryId(),
                r.getRiteCategory().getCategoryName(), r.getSummary(), imageUrl);
    }

    private RiteDetailDto toDetail(Rite r) {
        List<RiteStepDto> steps = r.getSteps().stream()
                .map(s -> new RiteStepDto(s.getStepOrder(), s.getStepTitle(), s.getStepText()))
                .toList();
        List<RiteImageDto> images = r.getMedia().stream()
                .map(m -> new RiteImageDto(m.getFilePath(), m.getCaption()))
                .toList();
        return new RiteDetailDto(r.getRiteId(), r.getTitle(), r.getRiteCategory().getCategoryName(),
                r.getSummary(), r.getDescription(), r.getSymbolism(), r.getSources(), steps, images);
    }
}

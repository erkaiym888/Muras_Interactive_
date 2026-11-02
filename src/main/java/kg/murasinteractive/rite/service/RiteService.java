package kg.murasinteractive.rite.service;

import kg.murasinteractive.rite.dto.*;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.repository.RiteCategoryRepository;
import kg.murasinteractive.rite.repository.RiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    /** Все опубликованные обряды для каталога. */
    @Transactional(readOnly = true)
    public List<RiteSummaryDto> findPublishedSummaries() {
        return riteRepository.findByStatusOrderByRiteIdAsc(Rite.STATUS_PUBLISHED).stream()
                .map(this::toSummary)
                .toList();
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
        return new RiteSummaryDto(r.getRiteId(), r.getTitle(), r.getRiteCategory().getCategoryName(),
                r.getSummary(), imageUrl);
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

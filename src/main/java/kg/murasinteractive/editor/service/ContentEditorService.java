package kg.murasinteractive.editor.service;

import kg.murasinteractive.editor.dto.CategoryRow;
import kg.murasinteractive.editor.dto.RiteForm;
import kg.murasinteractive.editor.dto.RiteRow;
import kg.murasinteractive.media.model.MediaFile;
import kg.murasinteractive.media.repository.MediaFileRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.model.RiteCategory;
import kg.murasinteractive.rite.model.RiteStep;
import kg.murasinteractive.rite.repository.RiteCategoryRepository;
import kg.murasinteractive.rite.repository.RiteRepository;
import kg.murasinteractive.user.exception.FieldValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Управление содержимым сайта (FR-7.1): добавление, просмотр, изменение и удаление
 * обрядов и их категорий. Доступ - только редактор контента и администратор (см. SecurityConfig).
 */
@Service
public class ContentEditorService {

    private final RiteRepository riteRepository;
    private final RiteCategoryRepository categoryRepository;
    private final MediaFileRepository mediaFileRepository;

    public ContentEditorService(RiteRepository riteRepository, RiteCategoryRepository categoryRepository,
                                MediaFileRepository mediaFileRepository) {
        this.riteRepository = riteRepository;
        this.categoryRepository = categoryRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    // ---------- Обряды ----------

    /** Все обряды в любом статусе, новые сверху. */
    @Transactional(readOnly = true)
    public List<RiteRow> listRites() {
        return riteRepository.findAllByOrderByRiteIdDesc().stream()
                .map(r -> new RiteRow(r.getRiteId(), r.getTitle(), r.getRiteCategory().getRiteCategoryId(),
                        r.getRiteCategory().getCategoryName(), r.getStatus(), r.getSteps().size(),
                        r.getMedia().isEmpty() ? null : r.getMedia().get(0).getFilePath()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RiteForm getRite(Long riteId) {
        return toForm(findRite(riteId));
    }

    @Transactional
    public RiteForm createRite(RiteForm form) {
        Rite rite = new Rite();
        apply(rite, form);
        return toForm(riteRepository.save(rite));
    }

    @Transactional
    public RiteForm updateRite(Long riteId, RiteForm form) {
        Rite rite = findRite(riteId);
        apply(rite, form);
        return toForm(riteRepository.saveAndFlush(rite));
    }

    @Transactional
    public void deleteRite(Long riteId) {
        riteRepository.delete(findRite(riteId));
    }

    private Rite findRite(Long riteId) {
        return riteRepository.findById(riteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обряд не найден"));
    }

    /** Переносит данные формы в обряд: поля, этапы (по порядку) и изображения. */
    private void apply(Rite rite, RiteForm form) {
        RiteCategory category = categoryRepository.findById(form.categoryId())
                .orElseThrow(() -> new FieldValidationException("categoryId", "Такой категории нет"));
        rite.setRiteCategory(category);
        rite.setTitle(form.title().trim());
        rite.setStatus(form.status());
        rite.setSummary(blankToNull(form.summary()));
        rite.setDescription(blankToNull(form.description()));
        rite.setSymbolism(blankToNull(form.symbolism()));
        rite.setSources(blankToNull(form.sources()));

        rite.getSteps().clear();
        int order = 1;
        for (RiteForm.Step step : nullToEmpty(form.steps())) {
            rite.addStep(order++, blankToNull(step.title()), step.text().trim());
        }

        // Уже сохранённые изображения с тем же адресом переиспользуем, новые - создаём.
        List<MediaFile> old = new ArrayList<>(rite.getMedia());
        rite.getMedia().clear();
        for (RiteForm.Image image : nullToEmpty(form.images())) {
            String url = image.url().trim();
            MediaFile media = old.stream()
                    .filter(m -> m.getFilePath().equals(url))
                    .findFirst()
                    .orElseGet(() -> new MediaFile(url, "IMAGE", null));
            old.remove(media);
            media.setCaption(blankToNull(image.caption()));
            rite.getMedia().add(mediaFileRepository.save(media));
        }
    }

    private RiteForm toForm(Rite r) {
        List<RiteForm.Step> steps = r.getSteps().stream()
                .map((RiteStep s) -> new RiteForm.Step(s.getStepTitle(), s.getStepText()))
                .toList();
        List<RiteForm.Image> images = r.getMedia().stream()
                .map(m -> new RiteForm.Image(m.getFilePath(), m.getCaption()))
                .toList();
        return new RiteForm(r.getRiteId(), r.getTitle(), r.getRiteCategory().getRiteCategoryId(), r.getStatus(),
                r.getSummary(), r.getDescription(), r.getSymbolism(), r.getSources(), steps, images);
    }

    // ---------- Категории ----------

    @Transactional(readOnly = true)
    public List<CategoryRow> listCategories() {
        return categoryRepository.findAllByOrderByRiteCategoryIdAsc().stream()
                .map(c -> new CategoryRow(c.getRiteCategoryId(), c.getCategoryName(),
                        riteRepository.countByRiteCategory_RiteCategoryId(c.getRiteCategoryId())))
                .toList();
    }

    @Transactional
    public CategoryRow createCategory(String name) {
        String clean = name.trim();
        checkUniqueName(clean, null);
        RiteCategory category = categoryRepository.save(new RiteCategory(clean));
        return new CategoryRow(category.getRiteCategoryId(), category.getCategoryName(), 0);
    }

    @Transactional
    public CategoryRow renameCategory(Long categoryId, String name) {
        RiteCategory category = findCategory(categoryId);
        String clean = name.trim();
        checkUniqueName(clean, categoryId);
        category.setCategoryName(clean);
        return new CategoryRow(categoryId, clean, riteRepository.countByRiteCategory_RiteCategoryId(categoryId));
    }

    /** Удалить можно только категорию без обрядов, чтобы материалы не потерялись. */
    @Transactional
    public void deleteCategory(Long categoryId) {
        RiteCategory category = findCategory(categoryId);
        if (riteRepository.existsByRiteCategory_RiteCategoryId(categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "В категории есть обряды. Сначала перенесите или удалите их.");
        }
        categoryRepository.delete(category);
    }

    private RiteCategory findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена"));
    }

    private void checkUniqueName(String name, Long exceptId) {
        boolean taken = categoryRepository.findAll().stream()
                .anyMatch(c -> !c.getRiteCategoryId().equals(exceptId) && c.getCategoryName().equalsIgnoreCase(name));
        if (taken) {
            throw new FieldValidationException("name", "Такая категория уже есть");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}

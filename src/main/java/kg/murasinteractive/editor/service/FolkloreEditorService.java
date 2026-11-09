package kg.murasinteractive.editor.service;

import kg.murasinteractive.editor.dto.FolkloreHeroForm;
import kg.murasinteractive.editor.dto.FolkloreRow;
import kg.murasinteractive.editor.dto.FolkloreWorkForm;
import kg.murasinteractive.editor.dto.ImageForm;
import kg.murasinteractive.folklore.dto.FolkloreTypeDto;
import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreType;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreTypeRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.folklore.service.FolkloreService;
import kg.murasinteractive.media.model.MediaFile;
import kg.murasinteractive.media.repository.MediaFileRepository;
import kg.murasinteractive.user.exception.FieldValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Управление фольклором (FR-7.1): добавление, просмотр, изменение и удаление героев и произведений,
 * связи «герой - произведение» и иллюстрации. Доступ - редактор контента и администратор.
 */
@Service
public class FolkloreEditorService {

    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;
    private final FolkloreTypeRepository typeRepository;
    private final MediaFileRepository mediaFileRepository;

    public FolkloreEditorService(FolkloreHeroRepository heroRepository, FolkloreWorkRepository workRepository,
                                 FolkloreTypeRepository typeRepository, MediaFileRepository mediaFileRepository) {
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
        this.typeRepository = typeRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    // ---------- Список и справочник ----------

    @Transactional(readOnly = true)
    public List<FolkloreTypeDto> types() {
        return typeRepository.findAllByOrderByFolkloreTypeIdAsc().stream()
                .map(t -> new FolkloreTypeDto(t.getFolkloreTypeId(), t.getTypeName()))
                .toList();
    }

    /** Все герои и произведения: сначала герои, потом произведения. */
    @Transactional(readOnly = true)
    public List<FolkloreRow> list() {
        List<FolkloreRow> rows = new ArrayList<>();
        for (FolkloreHero h : heroRepository.findAllByOrderByHeroIdAsc()) {
            rows.add(new FolkloreRow(FolkloreService.KIND_HERO, h.getHeroId(), h.getName(),
                    h.getHeroType().getFolkloreTypeId(), h.getHeroType().getTypeName(), h.getWorks().size(),
                    firstUrl(h.getMedia())));
        }
        for (FolkloreWork w : workRepository.findAllByOrderByWorkIdAsc()) {
            rows.add(new FolkloreRow(FolkloreService.KIND_WORK, w.getWorkId(), w.getTitle(),
                    w.getWorkType().getFolkloreTypeId(), w.getWorkType().getTypeName(), w.getHeroes().size(),
                    firstUrl(w.getMedia())));
        }
        return rows;
    }

    // ---------- Герои ----------

    @Transactional(readOnly = true)
    public FolkloreHeroForm getHero(Long heroId) {
        return toForm(findHero(heroId));
    }

    @Transactional
    public FolkloreHeroForm createHero(FolkloreHeroForm form) {
        FolkloreHero hero = new FolkloreHero();
        applyHero(hero, form);
        return toForm(heroRepository.save(hero));
    }

    @Transactional
    public FolkloreHeroForm updateHero(Long heroId, FolkloreHeroForm form) {
        FolkloreHero hero = findHero(heroId);
        applyHero(hero, form);
        return toForm(heroRepository.saveAndFlush(hero));
    }

    /** Связи hero_work и hero_media удаляются вместе с героем; произведения остаются. */
    @Transactional
    public void deleteHero(Long heroId) {
        heroRepository.delete(findHero(heroId));
    }

    private void applyHero(FolkloreHero hero, FolkloreHeroForm form) {
        hero.setName(form.name().trim());
        hero.setHeroType(findType(form.typeId()));
        hero.setDescription(blankToNull(form.description()));
        hero.getWorks().clear();
        for (Long workId : distinct(form.workIds())) {
            hero.getWorks().add(workRepository.findById(workId)
                    .orElseThrow(() -> new FieldValidationException("workIds", "Произведение не найдено")));
        }
        replaceMedia(hero.getMedia(), form.images());
    }

    private FolkloreHeroForm toForm(FolkloreHero h) {
        return new FolkloreHeroForm(h.getHeroId(), h.getName(), h.getHeroType().getFolkloreTypeId(),
                h.getDescription(), h.getWorks().stream().map(FolkloreWork::getWorkId).toList(),
                toImages(h.getMedia()));
    }

    private FolkloreHero findHero(Long heroId) {
        return heroRepository.findById(heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Герой не найден"));
    }

    // ---------- Произведения ----------

    @Transactional(readOnly = true)
    public FolkloreWorkForm getWork(Long workId) {
        return toForm(findWork(workId));
    }

    @Transactional
    public FolkloreWorkForm createWork(FolkloreWorkForm form) {
        FolkloreWork work = new FolkloreWork();
        applyWork(work, form);
        work = workRepository.save(work);
        linkHeroes(work, form.heroIds());
        return toForm(work);
    }

    @Transactional
    public FolkloreWorkForm updateWork(Long workId, FolkloreWorkForm form) {
        FolkloreWork work = findWork(workId);
        applyWork(work, form);
        linkHeroes(work, form.heroIds());
        return toForm(workRepository.saveAndFlush(work));
    }

    /** Сначала отвязываем героев (связь hero_work хранится на стороне героя), потом удаляем. */
    @Transactional
    public void deleteWork(Long workId) {
        FolkloreWork work = findWork(workId);
        for (FolkloreHero hero : new ArrayList<>(work.getHeroes())) {
            hero.getWorks().remove(work);
        }
        work.getHeroes().clear();
        workRepository.delete(work);
    }

    private void applyWork(FolkloreWork work, FolkloreWorkForm form) {
        work.setTitle(form.title().trim());
        work.setWorkType(findType(form.typeId()));
        work.setSummary(blankToNull(form.summary()));
        replaceMedia(work.getMedia(), form.images());
    }

    /** Связь хранится у героя, поэтому меняем списки произведений у самих героев. */
    private void linkHeroes(FolkloreWork work, List<Long> heroIds) {
        Set<Long> wanted = new HashSet<>(distinct(heroIds));
        for (FolkloreHero hero : new ArrayList<>(work.getHeroes())) {
            if (!wanted.remove(hero.getHeroId())) {
                hero.getWorks().remove(work);
                work.getHeroes().remove(hero);
            }
        }
        for (Long heroId : wanted) {
            FolkloreHero hero = heroRepository.findById(heroId)
                    .orElseThrow(() -> new FieldValidationException("heroIds", "Герой не найден"));
            hero.getWorks().add(work);
            work.getHeroes().add(hero);
        }
    }

    private FolkloreWorkForm toForm(FolkloreWork w) {
        return new FolkloreWorkForm(w.getWorkId(), w.getTitle(), w.getWorkType().getFolkloreTypeId(),
                w.getSummary(), w.getHeroes().stream().map(FolkloreHero::getHeroId).toList(),
                toImages(w.getMedia()));
    }

    private FolkloreWork findWork(Long workId) {
        return workRepository.findById(workId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Произведение не найдено"));
    }

    // ---------- Общее ----------

    private FolkloreType findType(Long typeId) {
        return typeRepository.findById(typeId)
                .orElseThrow(() -> new FieldValidationException("typeId", "Такого типа нет"));
    }

    /** Изображения с тем же адресом переиспользуем, новые создаём, порядок - как в форме. */
    private void replaceMedia(List<MediaFile> target, List<ImageForm> images) {
        List<MediaFile> old = new ArrayList<>(target);
        target.clear();
        for (ImageForm image : images == null ? List.<ImageForm>of() : images) {
            String url = image.url().trim();
            MediaFile media = old.stream()
                    .filter(m -> m.getFilePath().equals(url))
                    .findFirst()
                    .orElseGet(() -> new MediaFile(url, "IMAGE", null));
            old.remove(media);
            media.setCaption(blankToNull(image.caption()));
            target.add(mediaFileRepository.save(media));
        }
    }

    private static List<ImageForm> toImages(List<MediaFile> media) {
        return media.stream().map(m -> new ImageForm(m.getFilePath(), m.getCaption())).toList();
    }

    private static String firstUrl(List<MediaFile> media) {
        return media.isEmpty() ? null : media.get(0).getFilePath();
    }

    private static List<Long> distinct(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().distinct().toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

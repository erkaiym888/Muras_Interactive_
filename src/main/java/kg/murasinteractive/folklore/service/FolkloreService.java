package kg.murasinteractive.folklore.service;

import kg.murasinteractive.folklore.dto.*;
import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreType;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreTypeRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.i18n.service.Translatable;
import kg.murasinteractive.i18n.service.Translator;
import kg.murasinteractive.media.model.MediaFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class FolkloreService {

    public static final String KIND_HERO = "hero";
    public static final String KIND_WORK = "work";

    private final FolkloreTypeRepository typeRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;
    private final Translator translator;

    public FolkloreService(FolkloreTypeRepository typeRepository, FolkloreHeroRepository heroRepository,
                           FolkloreWorkRepository workRepository, Translator translator) {
        this.typeRepository = typeRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
        this.translator = translator;
    }

    /** Переводы всех типов разом - их немного (FR-8.1). */
    private Translator.Texts typeNames() {
        return translator.load(Translatable.FOLKLORE_TYPE,
                typeRepository.findAll().stream().map(FolkloreType::getFolkloreTypeId).toList());
    }

    private static String typeName(Translator.Texts names, FolkloreType type) {
        return names.get(type.getFolkloreTypeId(), "name", type.getTypeName());
    }

    /** Справочник типов - для кнопок-фильтров. */
    @Transactional(readOnly = true)
    public List<FolkloreTypeDto> findTypes() {
        Translator.Texts names = typeNames();
        return typeRepository.findAllByOrderByFolkloreTypeIdAsc().stream()
                .map(t -> new FolkloreTypeDto(t.getFolkloreTypeId(), typeName(names, t)))
                .toList();
    }

    /** Каталог: сначала герои, потом произведения. Только опубликованные (FR-7.2). */
    @Transactional(readOnly = true)
    public List<FolkloreItemDto> findItems() {
        List<FolkloreItemDto> items = new ArrayList<>();
        List<FolkloreHero> heroes = heroRepository.findAllByOrderByHeroIdAsc().stream().filter(FolkloreHero::isPublished).toList();
        List<FolkloreWork> works = workRepository.findAllByOrderByWorkIdAsc().stream().filter(FolkloreWork::isPublished).toList();
        Translator.Texts types = typeNames();
        Translator.Texts h = translator.load(Translatable.FOLKLORE_HERO, heroes.stream().map(FolkloreHero::getHeroId).toList());
        Translator.Texts w = translator.load(Translatable.FOLKLORE_WORK, works.stream().map(FolkloreWork::getWorkId).toList());
        for (FolkloreHero hero : heroes) {
            Long id = hero.getHeroId();
            items.add(new FolkloreItemDto(KIND_HERO, id, h.get(id, "name", hero.getName()),
                    typeName(types, hero.getHeroType()), h.get(id, "description", hero.getDescription()),
                    firstImageUrl(hero.getMedia())));
        }
        for (FolkloreWork work : works) {
            Long id = work.getWorkId();
            items.add(new FolkloreItemDto(KIND_WORK, id, w.get(id, "title", work.getTitle()),
                    typeName(types, work.getWorkType()), w.get(id, "summary", work.getSummary()),
                    firstImageUrl(work.getMedia())));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public FolkloreDetailDto getHero(Long heroId) {
        FolkloreHero hero = heroRepository.findById(heroId)
                .filter(FolkloreHero::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Герой не найден"));
        List<FolkloreWork> works = hero.getWorks().stream().filter(FolkloreWork::isPublished).toList();
        Translator.Texts wt = translator.load(Translatable.FOLKLORE_WORK, works.stream().map(FolkloreWork::getWorkId).toList());
        List<FolkloreLinkDto> related = works.stream()
                .map(w -> new FolkloreLinkDto(KIND_WORK, w.getWorkId(), wt.get(w.getWorkId(), "title", w.getTitle())))
                .toList();
        Translator.Texts t = translator.load(Translatable.FOLKLORE_HERO, List.of(heroId));
        return new FolkloreDetailDto(KIND_HERO, hero.getHeroId(), t.get(heroId, "name", hero.getName()),
                typeName(typeNames(), hero.getHeroType()), t.get(heroId, "description", hero.getDescription()),
                related, images(hero.getMedia()));
    }

    @Transactional(readOnly = true)
    public FolkloreDetailDto getWork(Long workId) {
        FolkloreWork work = workRepository.findById(workId)
                .filter(FolkloreWork::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Произведение не найдено"));
        List<FolkloreHero> heroes = work.getHeroes().stream().filter(FolkloreHero::isPublished).toList();
        Translator.Texts ht = translator.load(Translatable.FOLKLORE_HERO, heroes.stream().map(FolkloreHero::getHeroId).toList());
        List<FolkloreLinkDto> related = heroes.stream()
                .map(h -> new FolkloreLinkDto(KIND_HERO, h.getHeroId(), ht.get(h.getHeroId(), "name", h.getName())))
                .toList();
        Translator.Texts t = translator.load(Translatable.FOLKLORE_WORK, List.of(workId));
        return new FolkloreDetailDto(KIND_WORK, work.getWorkId(), t.get(workId, "title", work.getTitle()),
                typeName(typeNames(), work.getWorkType()), t.get(workId, "summary", work.getSummary()),
                related, images(work.getMedia()));
    }

    private static String firstImageUrl(List<MediaFile> media) {
        return media.isEmpty() ? null : media.get(0).getFilePath();
    }

    private static List<FolkloreImageDto> images(List<MediaFile> media) {
        return media.stream().map(m -> new FolkloreImageDto(m.getFilePath(), m.getCaption())).toList();
    }
}

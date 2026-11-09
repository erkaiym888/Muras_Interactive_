package kg.murasinteractive.folklore.service;

import kg.murasinteractive.folklore.dto.*;
import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreTypeRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
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

    public FolkloreService(FolkloreTypeRepository typeRepository, FolkloreHeroRepository heroRepository,
                           FolkloreWorkRepository workRepository) {
        this.typeRepository = typeRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
    }

    /** Справочник типов - для кнопок-фильтров. */
    @Transactional(readOnly = true)
    public List<FolkloreTypeDto> findTypes() {
        return typeRepository.findAllByOrderByFolkloreTypeIdAsc().stream()
                .map(t -> new FolkloreTypeDto(t.getFolkloreTypeId(), t.getTypeName()))
                .toList();
    }

    /** Каталог: сначала герои, потом произведения. */
    @Transactional(readOnly = true)
    public List<FolkloreItemDto> findItems() {
        List<FolkloreItemDto> items = new ArrayList<>();
        for (FolkloreHero hero : heroRepository.findAllByOrderByHeroIdAsc()) {
            items.add(new FolkloreItemDto(KIND_HERO, hero.getHeroId(), hero.getName(),
                    hero.getHeroType().getTypeName(), hero.getDescription(), firstImageUrl(hero.getMedia())));
        }
        for (FolkloreWork work : workRepository.findAllByOrderByWorkIdAsc()) {
            items.add(new FolkloreItemDto(KIND_WORK, work.getWorkId(), work.getTitle(),
                    work.getWorkType().getTypeName(), work.getSummary(), firstImageUrl(work.getMedia())));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public FolkloreDetailDto getHero(Long heroId) {
        FolkloreHero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Герой не найден"));
        List<FolkloreLinkDto> related = hero.getWorks().stream()
                .map(w -> new FolkloreLinkDto(KIND_WORK, w.getWorkId(), w.getTitle()))
                .toList();
        return new FolkloreDetailDto(KIND_HERO, hero.getHeroId(), hero.getName(),
                hero.getHeroType().getTypeName(), hero.getDescription(), related, images(hero.getMedia()));
    }

    @Transactional(readOnly = true)
    public FolkloreDetailDto getWork(Long workId) {
        FolkloreWork work = workRepository.findById(workId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Произведение не найдено"));
        List<FolkloreLinkDto> related = work.getHeroes().stream()
                .map(h -> new FolkloreLinkDto(KIND_HERO, h.getHeroId(), h.getName()))
                .toList();
        return new FolkloreDetailDto(KIND_WORK, work.getWorkId(), work.getTitle(),
                work.getWorkType().getTypeName(), work.getSummary(), related, images(work.getMedia()));
    }

    private static String firstImageUrl(List<MediaFile> media) {
        return media.isEmpty() ? null : media.get(0).getFilePath();
    }

    private static List<FolkloreImageDto> images(List<MediaFile> media) {
        return media.stream().map(m -> new FolkloreImageDto(m.getFilePath(), m.getCaption())).toList();
    }
}

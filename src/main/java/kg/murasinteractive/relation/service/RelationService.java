package kg.murasinteractive.relation.service;

import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.relation.dto.RelationNodeDto;
import kg.murasinteractive.relation.dto.RelationsDto;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.repository.RiteRepository;
import kg.murasinteractive.yurt.model.YurtElement;
import kg.murasinteractive.yurt.repository.YurtElementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Связи между сущностями (FR-3.3): герой ↔ произведение ↔ обряд ↔ элемент юрты.
 * Прямые связи хранятся в таблицах-связках; косвенные (например, обряд, связанный с произведением героя)
 * достраиваются здесь и помечаются полем via. Показываются только опубликованные обряды.
 */
@Service
public class RelationService {

    public static final String RITE = "rite";
    public static final String HERO = "hero";
    public static final String WORK = "work";
    public static final String YURT = "yurt";

    private final RiteRepository riteRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;
    private final YurtElementRepository yurtRepository;

    public RelationService(RiteRepository riteRepository, FolkloreHeroRepository heroRepository,
                           FolkloreWorkRepository workRepository, YurtElementRepository yurtRepository) {
        this.riteRepository = riteRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
        this.yurtRepository = yurtRepository;
    }

    @Transactional(readOnly = true)
    public RelationsDto forRite(Long riteId) {
        Rite rite = riteRepository.findByRiteIdAndStatus(riteId, Rite.STATUS_PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обряд не найден"));
        Nodes heroes = new Nodes(), works = new Nodes(), yurt = new Nodes();
        rite.getHeroes().forEach(h -> heroes.add(hero(h, null)));
        rite.getWorks().forEach(w -> works.add(work(w, null)));
        rite.getYurtElements().forEach(y -> yurt.add(yurt(y, null)));
        // Косвенно: произведения героев обряда и герои его произведений
        rite.getHeroes().forEach(h -> h.getWorks().forEach(w -> works.add(work(w, h.getName()))));
        rite.getWorks().forEach(w -> w.getHeroes().forEach(h -> heroes.add(hero(h, w.getTitle()))));
        return new RelationsDto(rite(rite, null), List.of(), heroes.list(), works.list(), yurt.list());
    }

    @Transactional(readOnly = true)
    public RelationsDto forHero(Long heroId) {
        FolkloreHero hero = heroRepository.findById(heroId)
                .filter(FolkloreHero::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Герой не найден"));
        Nodes rites = new Nodes(), works = new Nodes(), yurt = new Nodes();
        hero.getWorks().forEach(w -> works.add(work(w, null)));
        riteRepository.findByHeroes_HeroIdAndStatusOrderByTitleAsc(heroId, Rite.STATUS_PUBLISHED)
                .forEach(r -> rites.add(rite(r, null)));
        for (FolkloreWork w : hero.getWorks()) {
            riteRepository.findByWorks_WorkIdAndStatusOrderByTitleAsc(w.getWorkId(), Rite.STATUS_PUBLISHED)
                    .forEach(r -> rites.add(rite(r, w.getTitle())));
        }
        addYurtOfRites(rites, yurt);
        return new RelationsDto(hero(hero, null), rites.list(), List.of(), works.list(), yurt.list());
    }

    @Transactional(readOnly = true)
    public RelationsDto forWork(Long workId) {
        FolkloreWork work = workRepository.findById(workId)
                .filter(FolkloreWork::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Произведение не найдено"));
        Nodes rites = new Nodes(), heroes = new Nodes(), yurt = new Nodes();
        work.getHeroes().forEach(h -> heroes.add(hero(h, null)));
        riteRepository.findByWorks_WorkIdAndStatusOrderByTitleAsc(workId, Rite.STATUS_PUBLISHED)
                .forEach(r -> rites.add(rite(r, null)));
        for (FolkloreHero h : work.getHeroes()) {
            riteRepository.findByHeroes_HeroIdAndStatusOrderByTitleAsc(h.getHeroId(), Rite.STATUS_PUBLISHED)
                    .forEach(r -> rites.add(rite(r, h.getName())));
        }
        addYurtOfRites(rites, yurt);
        return new RelationsDto(work(work, null), rites.list(), heroes.list(), List.of(), yurt.list());
    }

    @Transactional(readOnly = true)
    public RelationsDto forYurtElement(String key) {
        YurtElement element = yurtRepository.findByElementKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Элемент юрты не найден"));
        Nodes rites = new Nodes(), heroes = new Nodes(), works = new Nodes();
        List<Rite> linked = riteRepository.findByYurtElements_ElementKeyAndStatusOrderByTitleAsc(key, Rite.STATUS_PUBLISHED);
        linked.forEach(r -> rites.add(rite(r, null)));
        // Косвенно: герои и произведения этих обрядов
        for (Rite r : linked) {
            r.getHeroes().forEach(h -> heroes.add(hero(h, r.getTitle())));
            r.getWorks().forEach(w -> works.add(work(w, r.getTitle())));
        }
        return new RelationsDto(yurt(element, null), rites.list(), heroes.list(), works.list(), List.of());
    }

    /** Элементы юрты, связанные с найденными обрядами (косвенная связь «через обряд»). */
    private void addYurtOfRites(Nodes rites, Nodes yurt) {
        for (RelationNodeDto r : rites.list()) {
            riteRepository.findById(r.id()).ifPresent(rite ->
                    rite.getYurtElements().forEach(y -> yurt.add(yurt(y, rite.getTitle()))));
        }
    }


    private static RelationNodeDto rite(Rite r, String via) {
        return new RelationNodeDto(RITE, r.getRiteId(), null, r.getTitle(), r.getRiteCategory().getCategoryName(), via);
    }

    /** Черновики и скрытые герои и произведения (FR-7.2) в связях не показываются: для них null, Nodes их пропускает. */
    private static RelationNodeDto hero(FolkloreHero h, String via) {
        if (!h.isPublished()) return null;
        return new RelationNodeDto(HERO, h.getHeroId(), null, h.getName(), h.getHeroType().getTypeName(), via);
    }

    private static RelationNodeDto work(FolkloreWork w, String via) {
        if (!w.isPublished()) return null;
        return new RelationNodeDto(WORK, w.getWorkId(), null, w.getTitle(), w.getWorkType().getTypeName(), via);
    }

    private static RelationNodeDto yurt(YurtElement y, String via) {
        return new RelationNodeDto(YURT, null, y.getElementKey(), y.getName(), y.getGroup(), via);
    }

    /** Список без повторов: прямая связь важнее косвенной, косвенная не затирает прямую. */
    private static final class Nodes {
        private final Map<String, RelationNodeDto> map = new LinkedHashMap<>();

        void add(RelationNodeDto node) {
            if (node == null) return;
            String id = node.kind() + ":" + (node.key() != null ? node.key() : node.id());
            RelationNodeDto existing = map.get(id);
            if (existing == null || (existing.via() != null && node.via() == null)) map.put(id, node);
        }

        List<RelationNodeDto> list() {
            List<RelationNodeDto> list = new ArrayList<>(map.values());
            // Сначала прямые связи, потом косвенные
            list.sort(Comparator.comparing((RelationNodeDto n) -> n.via() != null));
            return list;
        }
    }
}

package kg.murasinteractive.relation.service;

import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.repository.RiteRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Пример связи для демонстрации FR-3.3: обряд «Ас берүү» и эпос «Манас»
 * (в эпосе есть известный эпизод «Көкөтөйдүн ашы» - поминки по хану Көкөтөю, которые устраивает Манас).
 * Запускается после всех сидеров и только пока в базе нет ни одной связи обряда с фольклором,
 * поэтому не возвращает связи, которые редактор удалил. Остальные связи расставляет редактор.
 */
@Component
public class RelationSeeder {

    private final RiteRepository riteRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;

    public RelationSeeder(RiteRepository riteRepository, FolkloreHeroRepository heroRepository,
                          FolkloreWorkRepository workRepository) {
        this.riteRepository = riteRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        boolean anyLinks = riteRepository.findAll().stream()
                .anyMatch(r -> !r.getHeroes().isEmpty() || !r.getWorks().isEmpty());
        if (anyLinks) return;
        Rite as = riteRepository.findAll().stream()
                .filter(r -> "Ас берүү".equals(r.getTitle())).findFirst().orElse(null);
        if (as == null) return;
        workRepository.findAll().stream().filter(w -> "Манас".equals(w.getTitle())).findFirst()
                .ifPresent(as.getWorks()::add);
        heroRepository.findAll().stream().filter(h -> "Манас".equals(h.getName())).findFirst()
                .ifPresent(as.getHeroes()::add);
    }
}

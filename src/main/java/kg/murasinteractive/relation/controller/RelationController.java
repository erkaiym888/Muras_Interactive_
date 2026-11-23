package kg.murasinteractive.relation.controller;

import kg.murasinteractive.relation.dto.RelationsDto;
import kg.murasinteractive.relation.service.RelationService;
import org.springframework.web.bind.annotation.*;

/**
 * Связи между разделами (FR-3.3), открыто для гостей.
 *  GET /api/relations/rite/{id}
 *  GET /api/relations/hero/{id}
 *  GET /api/relations/work/{id}
 *  GET /api/relations/yurt/{key}   - key элемента схемы: tunduk, kerege...
 */
@RestController
@RequestMapping("/api/relations")
public class RelationController {

    private final RelationService relationService;

    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @GetMapping("/rite/{id}")
    public RelationsDto rite(@PathVariable Long id) { return relationService.forRite(id); }

    @GetMapping("/hero/{id}")
    public RelationsDto hero(@PathVariable Long id) { return relationService.forHero(id); }

    @GetMapping("/work/{id}")
    public RelationsDto work(@PathVariable Long id) { return relationService.forWork(id); }

    @GetMapping("/yurt/{key}")
    public RelationsDto yurt(@PathVariable String key) { return relationService.forYurtElement(key); }
}

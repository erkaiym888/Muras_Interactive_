package kg.murasinteractive.rite.controller;

import kg.murasinteractive.rite.dto.RiteCategoryDto;
import kg.murasinteractive.rite.dto.RiteDetailDto;
import kg.murasinteractive.rite.dto.RiteSummaryDto;
import kg.murasinteractive.rite.service.RiteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * JSON API для React-страниц из папки design/.
 *  GET /api/rite-categories  - категории (для фильтров)
 *  GET /api/rites            - каталог обрядов
 *  GET /api/rites/{id}       - карточка обряда
 */
@RestController
@RequestMapping("/api")
public class RiteApiController {

    private final RiteService riteService;

    public RiteApiController(RiteService riteService) {
        this.riteService = riteService;
    }

    @GetMapping("/rite-categories")
    public List<RiteCategoryDto> categories() {
        return riteService.findCategories();
    }

    @GetMapping("/rites")
    public List<RiteSummaryDto> rites() {
        return riteService.findPublishedSummaries();
    }

    @GetMapping("/rites/{riteId}")
    public RiteDetailDto rite(@PathVariable Long riteId) {
        return riteService.getPublishedDetail(riteId);
    }
}

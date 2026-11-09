package kg.murasinteractive.folklore.controller;

import kg.murasinteractive.folklore.dto.FolkloreDetailDto;
import kg.murasinteractive.folklore.dto.FolkloreItemDto;
import kg.murasinteractive.folklore.dto.FolkloreTypeDto;
import kg.murasinteractive.folklore.service.FolkloreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * JSON API раздела «Фольклор».
 *  GET /api/folklore/types        - справочник типов
 *  GET /api/folklore/items        - каталог героев и произведений
 *  GET /api/folklore/heroes/{id}  - карточка героя
 *  GET /api/folklore/works/{id}   - карточка произведения
 */
@RestController
@RequestMapping("/api/folklore")
public class FolkloreApiController {

    private final FolkloreService folkloreService;

    public FolkloreApiController(FolkloreService folkloreService) {
        this.folkloreService = folkloreService;
    }

    @GetMapping("/types")
    public List<FolkloreTypeDto> types() {
        return folkloreService.findTypes();
    }

    @GetMapping("/items")
    public List<FolkloreItemDto> items() {
        return folkloreService.findItems();
    }

    @GetMapping("/heroes/{heroId}")
    public FolkloreDetailDto hero(@PathVariable Long heroId) {
        return folkloreService.getHero(heroId);
    }

    @GetMapping("/works/{workId}")
    public FolkloreDetailDto work(@PathVariable Long workId) {
        return folkloreService.getWork(workId);
    }
}

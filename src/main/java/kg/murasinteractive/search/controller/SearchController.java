package kg.murasinteractive.search.controller;

import kg.murasinteractive.search.dto.SearchResponseDto;
import kg.murasinteractive.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/search?q=невеста&limit=8 - поиск по всем разделам (FR-4.1), открыт для гостей.
 * limit - сколько вариантов вернуть (1-50, по умолчанию 50); total больше числа результатов, если есть ещё.
 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public SearchResponseDto search(@RequestParam(name = "q", required = false) String q,
                                    @RequestParam(name = "limit", required = false) Integer limit) {
        return searchService.search(q, limit);
    }
}

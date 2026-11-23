package kg.murasinteractive.search.dto;

import java.util.List;
import java.util.Map;

/**
 * Ответ поиска.
 *
 * @param counts    сколько найдено каждого вида материала (rite, hero, work, yurt) - для групп и фильтра (FR-4.3)
 * @param corrected исправленный запрос, если в словах были опечатки и результаты найдены по нему (FR-4.2);
 *                  null - запрос не исправлялся
 */
public record SearchResponseDto(String query, String corrected, int total, Map<String, Integer> counts,
                                List<SearchResultDto> results) {
}

package kg.murasinteractive.search.dto;

import java.util.List;

/**
 * Ответ поиска.
 *
 * @param corrected исправленный запрос, если в словах были опечатки и результаты найдены по нему (FR-4.2);
 *                  null - запрос не исправлялся
 */
public record SearchResponseDto(String query, String corrected, int total, List<SearchResultDto> results) {
}

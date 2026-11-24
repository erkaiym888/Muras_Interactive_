package kg.murasinteractive.search.dto;

import java.util.List;

public record SearchResponseDto(String query, int total, List<SearchResultDto> results) {
}

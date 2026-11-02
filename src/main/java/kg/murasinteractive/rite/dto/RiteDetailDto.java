package kg.murasinteractive.rite.dto;

import java.util.List;

/** Полная карточка обряда (FR-2.2). */
public record RiteDetailDto(Long id, String title, String category, String summary, String description,
                            String symbolism, String sources,
                            List<RiteStepDto> steps, List<RiteImageDto> images) {
}

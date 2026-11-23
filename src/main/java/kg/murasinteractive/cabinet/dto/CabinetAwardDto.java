package kg.murasinteractive.cabinet.dto;

import java.time.Instant;

/** Полученная награда: значок или уровень. Заполняется наградами (FR-5.3). */
public record CabinetAwardDto(String title, String description, Instant awardedAt) {
}

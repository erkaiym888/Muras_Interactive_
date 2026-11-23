package kg.murasinteractive.awards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Открыт материал: type - YURT_PART, RITE, HERO, WORK или SEARCH; key - ключ элемента или id карточки. */
public record ViewRequest(
        @NotBlank @Pattern(regexp = "YURT_PART|RITE|HERO|WORK|SEARCH") String type,
        @NotBlank @Size(max = 200) String key) {
}

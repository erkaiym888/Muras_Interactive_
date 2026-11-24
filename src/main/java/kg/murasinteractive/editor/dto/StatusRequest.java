package kg.murasinteractive.editor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kg.murasinteractive.content.ContentStatus;

/** Смена статуса материала одним действием (FR-7.2). */
public record StatusRequest(
        @NotBlank(message = "Выберите статус")
        @Pattern(regexp = ContentStatus.PATTERN, message = "Статус: черновик, опубликован или скрыт")
        String status) {
}

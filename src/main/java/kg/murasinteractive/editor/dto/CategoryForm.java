package kg.murasinteractive.editor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Название категории обрядов при создании и переименовании. */
public record CategoryForm(
        @NotBlank(message = "Введите название категории")
        @Size(max = 100, message = "Название не должно быть длиннее 100 символов")
        String name) {
}

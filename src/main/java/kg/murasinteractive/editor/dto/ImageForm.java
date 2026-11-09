package kg.murasinteractive.editor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Изображение материала: адрес и подпись (alt-текст). */
public record ImageForm(
        @NotBlank(message = "Укажите адрес изображения")
        @Size(max = 500, message = "Адрес не должен быть длиннее 500 символов")
        String url,

        @Size(max = 500, message = "Подпись не должна быть длиннее 500 символов")
        String caption) {
}

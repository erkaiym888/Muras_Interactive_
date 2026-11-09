package kg.murasinteractive.editor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Обряд в редакторе контента (FR-7.1): приходит при создании и изменении,
 * и в таком же виде отдаётся для заполнения формы (id при создании не нужен).
 */
public record RiteForm(
        Long id,

        @NotBlank(message = "Введите название")
        @Size(max = 200, message = "Название не должно быть длиннее 200 символов")
        String title,

        @NotNull(message = "Выберите категорию")
        Long categoryId,

        @NotBlank(message = "Выберите статус")
        @Pattern(regexp = "DRAFT|PUBLISHED|HIDDEN", message = "Статус: черновик, опубликован или скрыт")
        String status,

        @Size(max = 500, message = "Аннотация не должна быть длиннее 500 символов")
        String summary,

        String description,
        String symbolism,
        String sources,

        @Valid
        @Size(max = 50, message = "Не больше 50 этапов")
        List<Step> steps,

        @Valid
        @Size(max = 20, message = "Не больше 20 изображений")
        List<Image> images) {

    public record Step(
            @Size(max = 200, message = "Название этапа не должно быть длиннее 200 символов")
            String title,

            @NotBlank(message = "Опишите этап")
            String text) {
    }

    public record Image(
            @NotBlank(message = "Укажите адрес изображения")
            @Size(max = 500, message = "Адрес не должен быть длиннее 500 символов")
            String url,

            @Size(max = 500, message = "Подпись не должна быть длиннее 500 символов")
            String caption) {
    }
}

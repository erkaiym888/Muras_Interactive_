package kg.murasinteractive.editor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import kg.murasinteractive.content.ContentStatus;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Произведение фольклора в редакторе (FR-7.1): эпос, сказка, легенда, пословицы. */
public record FolkloreWorkForm(
        Long id,

        @NotBlank(message = "Введите название")
        @Size(max = 200, message = "Название не должно быть длиннее 200 символов")
        String title,

        @NotNull(message = "Выберите тип")
        Long typeId,

        String summary,

        /** Статус (FR-7.2); если не указан - у нового материала «черновик», у существующего не меняется. */
        @Pattern(regexp = ContentStatus.PATTERN, message = "Статус: черновик, опубликован или скрыт")
        String status,

        /** Герои произведения. */
        List<Long> heroIds,

        @Valid
        @Size(max = 20, message = "Не больше 20 изображений")
        List<ImageForm> images) {
}

package kg.murasinteractive.editor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Герой фольклора в редакторе (FR-7.1): приходит при создании/изменении и отдаётся для формы. */
public record FolkloreHeroForm(
        Long id,

        @NotBlank(message = "Введите имя героя")
        @Size(max = 200, message = "Имя не должно быть длиннее 200 символов")
        String name,

        @NotNull(message = "Выберите тип")
        Long typeId,

        String description,

        /** Произведения, в которых встречается герой. */
        List<Long> workIds,

        @Valid
        @Size(max = 20, message = "Не больше 20 изображений")
        List<ImageForm> images) {
}

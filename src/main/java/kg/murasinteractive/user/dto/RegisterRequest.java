package kg.murasinteractive.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Введите имя")
        @Size(max = 100, message = "Имя не должно быть длиннее 100 символов")
        String displayName,

        @NotBlank(message = "Введите адрес электронной почты")
        @Email(message = "Введите корректный адрес электронной почты")
        @Size(max = 255, message = "Адрес слишком длинный")
        String email,

        @NotBlank(message = "Введите пароль")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password) {
}

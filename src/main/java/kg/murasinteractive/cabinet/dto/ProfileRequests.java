package kg.murasinteractive.cabinet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Запросы редактирования профиля в личном кабинете (FR-6.2). */
public final class ProfileRequests {

    /** Новый никнейм. */
    public record NameRequest(
            @NotBlank(message = "Введите никнейм")
            @Size(min = 2, max = 40, message = "Никнейм - от 2 до 40 символов")
            String displayName) {}

    /** Новая почта; для подтверждения - текущий пароль. */
    public record EmailRequest(
            @NotBlank(message = "Введите адрес почты")
            @Email(message = "Проверьте адрес почты")
            @Size(max = 255, message = "Адрес слишком длинный")
            String email,
            @NotBlank(message = "Введите текущий пароль")
            String currentPassword) {}

    /** Аватар как data URL (data:image/jpeg;base64,...), уже уменьшенный в браузере. */
    public record AvatarRequest(@NotBlank(message = "Выберите картинку") String dataUrl) {}

    private ProfileRequests() {}
}

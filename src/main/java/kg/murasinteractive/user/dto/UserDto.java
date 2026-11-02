package kg.murasinteractive.user.dto;

/** Данные пользователя для интерфейса. Хэш пароля сюда никогда не попадает. */
public record UserDto(Long id, String email, String displayName, String role) {
}

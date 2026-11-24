package kg.murasinteractive.admin.dto;

import java.time.Instant;

/** Строка списка пользователей в панели администратора (FR-7.3). Хэш пароля сюда не попадает. */
public record AdminUserRow(Long id, String email, String displayName, String avatarUrl, String role, boolean blocked,
                           int totalPoints, Instant registeredAt, boolean me) {
}

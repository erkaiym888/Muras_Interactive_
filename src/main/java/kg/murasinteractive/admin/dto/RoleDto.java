package kg.murasinteractive.admin.dto;

/** Роль с подписью и числом пользователей (FR-7.3). */
public record RoleDto(String name, String title, String description, long userCount) {
}

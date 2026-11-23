package kg.murasinteractive.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleRequest(
        @NotBlank(message = "Выберите роль")
        @Pattern(regexp = "USER|EDITOR|ADMIN", message = "Роль: пользователь, редактор или администратор")
        String role) {
}

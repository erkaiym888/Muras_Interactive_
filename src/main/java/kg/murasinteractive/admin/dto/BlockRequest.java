package kg.murasinteractive.admin.dto;

import jakarta.validation.constraints.NotNull;

public record BlockRequest(@NotNull(message = "Укажите, блокировать ли пользователя") Boolean blocked) {
}

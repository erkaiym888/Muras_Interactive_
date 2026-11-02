package kg.murasinteractive.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Введите адрес электронной почты")
        @Email(message = "Введите корректный адрес электронной почты")
        String email) {
}

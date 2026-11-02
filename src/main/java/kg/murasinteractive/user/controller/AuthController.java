package kg.murasinteractive.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kg.murasinteractive.user.dto.*;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.service.PasswordResetService;
import kg.murasinteractive.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/**
 * API учётных записей (FR-6.1).
 *  GET  /api/auth/csrf     - токен защиты от CSRF (нужен перед каждым POST)
 *  GET  /api/auth/me       - текущий пользователь (204, если не вошёл)
 *  POST /api/auth/register - регистрация
 *  POST /api/auth/login    - вход (обрабатывает Spring Security, см. SecurityConfig)
 *  POST /api/auth/logout   - выход (обрабатывает Spring Security)
 *  POST /api/auth/forgot   - письмо со ссылкой для восстановления
 *  POST /api/auth/reset    - новый пароль по ссылке из письма
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/csrf")
    public CsrfDto csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            throw new IllegalStateException("CSRF-защита не включена");
        }
        return new CsrfDto(token.getToken(), token.getHeaderName());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return ResponseEntity.noContent().build();
        }
        return userService.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(UserService.toDto(user)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = userService.register(request.displayName().trim(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserService.toDto(user));
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }
}

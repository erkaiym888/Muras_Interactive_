package kg.murasinteractive.user.service;

import kg.murasinteractive.user.exception.InvalidResetTokenException;
import kg.murasinteractive.user.model.PasswordResetToken;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.PasswordResetTokenRepository;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Восстановление пароля по ссылке из письма (FR-6.1). */
@Service
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final SecureRandom random = new SecureRandom();
    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String frontendUrl;

    public PasswordResetService(UserAccountRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                @Value("${app.frontend-url:http://localhost:8443}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    /** Если такой адрес есть, создаёт ссылку и отправляет письмо. Ответ клиенту всегда одинаковый. */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(UserService.normalizeEmail(email)).ifPresent(user -> {
            tokenRepository.deleteByUserAndUsedAtIsNull(user);
            String rawToken = newToken();
            tokenRepository.save(new PasswordResetToken(user, hash(rawToken), Instant.now().plus(TOKEN_LIFETIME)));
            mailService.sendPasswordReset(user.getEmail(), frontendUrl + "/?reset=" + rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        UserService.checkPasswordLength(newPassword);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidResetTokenException::new);
        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

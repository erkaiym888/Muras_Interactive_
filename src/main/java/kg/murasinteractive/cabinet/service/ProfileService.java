package kg.murasinteractive.cabinet.service;

import kg.murasinteractive.user.exception.EmailAlreadyUsedException;
import kg.murasinteractive.user.exception.FieldValidationException;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import kg.murasinteractive.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Редактирование профиля в личном кабинете (FR-6.2): никнейм, почта, аватар.
 * Почта меняется только с текущим паролем - чтобы аккаунт не увели с чужого открытого компьютера.
 */
@Service
public class ProfileService {

    /** Аватар после уменьшения в браузере весит десятки килобайт; с запасом - не больше 300 КБ. */
    public static final int MAX_AVATAR_BYTES = 300 * 1024;
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=\\s]+)$");

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount rename(UserAccount user, String displayName) {
        UserAccount u = reload(user);
        u.setDisplayName(displayName.trim().replaceAll("\\s+", " "));
        return u;
    }

    @Transactional
    public UserAccount changeEmail(UserAccount user, String email, String currentPassword) {
        UserAccount u = reload(user);
        if (!passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
            throw new FieldValidationException("currentPassword", "Неверный пароль");
        }
        String normalized = UserService.normalizeEmail(email);
        if (normalized.equals(u.getEmail())) return u;
        if (userRepository.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException();
        }
        u.setEmail(normalized);
        return u;
    }

    @Transactional
    public UserAccount setAvatar(UserAccount user, String dataUrl) {
        Matcher m = DATA_URL.matcher(dataUrl.trim());
        if (!m.matches()) {
            throw new FieldValidationException("avatar", "Нужна картинка JPG, PNG или WebP");
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new FieldValidationException("avatar", "Не удалось прочитать картинку");
        }
        if (bytes.length == 0 || bytes.length > MAX_AVATAR_BYTES) {
            throw new FieldValidationException("avatar", "Картинка слишком большая");
        }
        String type = m.group(1);
        if (!signatureMatches(type, bytes)) {
            throw new FieldValidationException("avatar", "Файл не похож на картинку");
        }
        UserAccount u = reload(user);
        u.setAvatar(bytes, type);
        return u;
    }

    @Transactional
    public UserAccount removeAvatar(UserAccount user) {
        UserAccount u = reload(user);
        u.setAvatar(null, null);
        return u;
    }

    /** Проверяем первые байты файла: заголовок data URL можно подделать. */
    static boolean signatureMatches(String type, byte[] b) {
        Map<String, int[]> magic = Map.of(
                "image/jpeg", new int[]{0xFF, 0xD8, 0xFF},
                "image/png", new int[]{0x89, 0x50, 0x4E, 0x47},
                "image/webp", new int[]{0x52, 0x49, 0x46, 0x46});
        int[] sig = magic.get(type);
        if (sig == null || b.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if ((b[i] & 0xFF) != sig[i]) return false;
        }
        return true;
    }

    private UserAccount reload(UserAccount user) {
        return userRepository.findById(user.getUserId()).orElseThrow();
    }
}

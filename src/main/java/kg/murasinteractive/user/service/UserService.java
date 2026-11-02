package kg.murasinteractive.user.service;

import kg.murasinteractive.user.dto.UserDto;
import kg.murasinteractive.user.exception.EmailAlreadyUsedException;
import kg.murasinteractive.user.exception.FieldValidationException;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.model.UserRole;
import kg.murasinteractive.user.repository.UserAccountRepository;
import kg.murasinteractive.user.repository.UserRoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userRepository, UserRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Адрес почты хранится в нижнем регистре и без пробелов по краям. */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** BCrypt принимает не более 72 байт; кириллица занимает 2 байта на символ. */
    public static void checkPasswordLength(String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new FieldValidationException("password", "Пароль слишком длинный, сократите его");
        }
    }

    public static UserDto toDto(UserAccount user) {
        return new UserDto(user.getUserId(), user.getEmail(), user.getDisplayName(),
                user.getRole().getRoleName());
    }

    @Transactional
    public UserAccount register(String displayName, String email, String rawPassword) {
        checkPasswordLength(rawPassword);
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException();
        }
        UserRole role = roleRepository.findByRoleName(UserRole.USER)
                .orElseGet(() -> roleRepository.save(new UserRole(UserRole.USER)));

        UserAccount user = new UserAccount();
        user.setEmail(normalized);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // два запроса с одним адресом одновременно
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }
}

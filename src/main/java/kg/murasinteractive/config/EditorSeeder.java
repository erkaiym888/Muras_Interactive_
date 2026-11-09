package kg.murasinteractive.config;

import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.model.UserRole;
import kg.murasinteractive.user.repository.UserAccountRepository;
import kg.murasinteractive.user.repository.UserRoleRepository;
import kg.murasinteractive.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Роли и первая учётная запись редактора контента (FR-7.1).
 * Через обычную регистрацию получают только роль USER, поэтому редактора создаём здесь
 * из настроек app.editor.* (переменные EDITOR_EMAIL, EDITOR_PASSWORD, EDITOR_NAME).
 * Если пользователь с таким адресом уже есть, ему просто выдаётся роль EDITOR.
 */
@Component
public class EditorSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EditorSeeder.class);

    private final UserRoleRepository roleRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public EditorSeeder(UserRoleRepository roleRepository, UserAccountRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.editor.email}") String email,
                        @Value("${app.editor.password}") String password,
                        @Value("${app.editor.name}") String displayName) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String name : List.of(UserRole.USER, UserRole.EDITOR, UserRole.ADMIN)) {
            if (roleRepository.findByRoleName(name).isEmpty()) {
                roleRepository.save(new UserRole(name));
            }
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Редактор контента не создан: не заданы EDITOR_EMAIL и EDITOR_PASSWORD");
            return;
        }

        UserRole editorRole = roleRepository.findByRoleName(UserRole.EDITOR).orElseThrow();
        String normalized = UserService.normalizeEmail(email);
        UserAccount editor = userRepository.findByEmail(normalized).orElse(null);
        if (editor == null) {
            UserService.checkPasswordLength(password);
            editor = new UserAccount();
            editor.setEmail(normalized);
            editor.setDisplayName(displayName);
            editor.setPasswordHash(passwordEncoder.encode(password));
            editor.setRole(editorRole);
            userRepository.save(editor);
            log.info("Создан редактор контента: {}", normalized);
        } else if (UserRole.USER.equals(editor.getRole().getRoleName())) {
            editor.setRole(editorRole);
            log.info("Пользователю {} выдана роль редактора контента", normalized);
        }
    }
}

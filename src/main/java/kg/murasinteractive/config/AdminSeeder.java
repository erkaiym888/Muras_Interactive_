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
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Первый администратор (FR-7.3) из настроек app.admin.* (ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NAME).
 * Дальше роли раздаёт уже он сам в разделе «Пользователи».
 * Если пользователь с таким адресом уже есть, ему выдаётся роль ADMIN и снимается блокировка.
 */
@Component
@Order(20)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String DEFAULT_NAME = "Администратор";

    private final UserRoleRepository roleRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminSeeder(UserRoleRepository roleRepository, UserAccountRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.password:}") String password,
                       @Value("${app.admin.name:}") String displayName) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.displayName = displayName == null || displayName.isBlank() ? DEFAULT_NAME : displayName.trim();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Администратор не создан: не заданы ADMIN_EMAIL и ADMIN_PASSWORD");
            return;
        }
        UserRole adminRole = roleRepository.findByRoleName(UserRole.ADMIN)
                .orElseGet(() -> roleRepository.save(new UserRole(UserRole.ADMIN)));
        String normalized = UserService.normalizeEmail(email);
        UserAccount admin = userRepository.findByEmail(normalized).orElse(null);
        if (admin == null) {
            UserService.checkPasswordLength(password);
            admin = new UserAccount();
            admin.setEmail(normalized);
            admin.setDisplayName(displayName);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(adminRole);
            userRepository.save(admin);
            log.info("Создан администратор: {}", normalized);
        } else if (!UserRole.ADMIN.equals(admin.getRole().getRoleName()) || admin.isBlocked()) {
            admin.setRole(adminRole);
            admin.setBlocked(false);
            log.info("Пользователю {} выдана роль администратора", normalized);
        }
    }
}

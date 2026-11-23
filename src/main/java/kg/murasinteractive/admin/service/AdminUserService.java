package kg.murasinteractive.admin.service;

import kg.murasinteractive.admin.dto.AdminUserRow;
import kg.murasinteractive.admin.dto.RoleDto;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.model.UserRole;
import kg.murasinteractive.user.repository.UserAccountRepository;
import kg.murasinteractive.user.repository.UserRoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Управление пользователями и ролями (FR-7.3).
 * Правила: администратор не меняет роль и не блокирует сам себя,
 * и в системе всегда остаётся хотя бы один активный администратор.
 */
@Service
public class AdminUserService {

    /** Порядок ролей в интерфейсе и подписи к ним. */
    public static final List<String> ROLES = List.of(UserRole.USER, UserRole.EDITOR, UserRole.ADMIN);
    private static final Map<String, String> TITLES = Map.of(
            UserRole.USER, "Пользователь",
            UserRole.EDITOR, "Редактор контента",
            UserRole.ADMIN, "Администратор");
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            UserRole.USER, "Викторины, личный кабинет, награды и рейтинг",
            UserRole.EDITOR, "Добавляет и редактирует материалы, вопросы и медиафайлы",
            UserRole.ADMIN, "Всё, что у редактора, плюс пользователи, роли и справочники");

    private final UserAccountRepository userRepository;
    private final UserRoleRepository roleRepository;

    public AdminUserService(UserAccountRepository userRepository, UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /** Список с поиском по имени/почте и фильтром по роли; новые сверху. */
    @Transactional(readOnly = true)
    public List<AdminUserRow> users(String query, String role, String currentEmail) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "registeredAt")).stream()
                .filter(u -> role == null || role.isBlank() || role.equals(u.getRole().getRoleName()))
                .filter(u -> q.isEmpty() || u.getEmail().contains(q)
                        || u.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
                .map(u -> toRow(u, currentEmail))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDto> roles() {
        return ROLES.stream()
                .map(r -> new RoleDto(r, TITLES.get(r), DESCRIPTIONS.get(r), userRepository.countByRole_RoleName(r)))
                .toList();
    }

    @Transactional
    public AdminUserRow changeRole(Long userId, String roleName, String currentEmail) {
        UserAccount user = find(userId);
        if (user.getEmail().equals(currentEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя изменить собственную роль");
        }
        String current = user.getRole().getRoleName();
        if (current.equals(roleName)) return toRow(user, currentEmail);
        if (UserRole.ADMIN.equals(current) && !user.isBlocked() && lastActiveAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Это последний администратор — сначала назначьте другого");
        }
        UserRole role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Такой роли нет"));
        user.setRole(role);
        return toRow(user, currentEmail);
    }

    @Transactional
    public AdminUserRow setBlocked(Long userId, boolean blocked, String currentEmail) {
        UserAccount user = find(userId);
        if (user.getEmail().equals(currentEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя заблокировать самого себя");
        }
        if (blocked && !user.isBlocked() && UserRole.ADMIN.equals(user.getRole().getRoleName()) && lastActiveAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Это последний администратор — его нельзя заблокировать");
        }
        user.setBlocked(blocked);
        return toRow(user, currentEmail);
    }

    private boolean lastActiveAdmin() {
        return userRepository.countByRole_RoleNameAndBlockedFalse(UserRole.ADMIN) <= 1;
    }

    private UserAccount find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    private static AdminUserRow toRow(UserAccount u, String currentEmail) {
        return new AdminUserRow(u.getUserId(), u.getEmail(), u.getDisplayName(), u.getRole().getRoleName(),
                u.isBlocked(), u.getTotalPoints(), u.getRegisteredAt(), u.getEmail().equals(currentEmail));
    }
}

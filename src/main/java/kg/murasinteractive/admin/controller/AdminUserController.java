package kg.murasinteractive.admin.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.admin.dto.AdminUserRow;
import kg.murasinteractive.admin.dto.BlockRequest;
import kg.murasinteractive.admin.dto.RoleDto;
import kg.murasinteractive.admin.dto.RoleRequest;
import kg.murasinteractive.admin.service.AdminUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Управление пользователями и ролями (FR-7.3), только для роли ADMIN (см. SecurityConfig).
 *  GET   /api/admin/users?q=&role=        - список пользователей
 *  GET   /api/admin/roles                 - роли и число пользователей в каждой
 *  PATCH /api/admin/users/{id}/role       - сменить роль
 *  PATCH /api/admin/users/{id}/blocked    - заблокировать или разблокировать
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public List<AdminUserRow> users(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String role,
                                    Authentication authentication) {
        return adminUserService.users(q, role, authentication.getName());
    }

    @GetMapping("/roles")
    public List<RoleDto> roles() {
        return adminUserService.roles();
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserRow role(@PathVariable Long userId, @Valid @RequestBody RoleRequest request,
                             Authentication authentication) {
        return adminUserService.changeRole(userId, request.role(), authentication.getName());
    }

    @PatchMapping("/users/{userId}/blocked")
    public AdminUserRow blocked(@PathVariable Long userId, @Valid @RequestBody BlockRequest request,
                                Authentication authentication) {
        return adminUserService.setBlocked(userId, request.blocked(), authentication.getName());
    }
}

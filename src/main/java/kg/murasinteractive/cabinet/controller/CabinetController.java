package kg.murasinteractive.cabinet.controller;

import kg.murasinteractive.cabinet.dto.CabinetDto;
import kg.murasinteractive.cabinet.service.CabinetService;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/cabinet - данные личного кабинета любого вошедшего пользователя (любая роль). */
@RestController
@RequestMapping("/api/cabinet")
public class CabinetController {

    private final CabinetService cabinetService;
    private final CurrentUserService currentUser;

    public CabinetController(CabinetService cabinetService, CurrentUserService currentUser) {
        this.cabinetService = cabinetService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public CabinetDto cabinet(Authentication authentication) {
        return cabinetService.build(currentUser.require(authentication));
    }
}

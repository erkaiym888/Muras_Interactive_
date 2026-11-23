package kg.murasinteractive.awards.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.awards.dto.AwardsDto;
import kg.murasinteractive.awards.dto.RewardEventDto;
import kg.murasinteractive.awards.dto.ViewRequest;
import kg.murasinteractive.awards.service.AwardService;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Награды и активность (FR-5.3). Только для вошедших (SecurityConfig).
 *  GET  /api/awards          - уровень, прогресс до следующего, все уровни и значки
 *  POST /api/activity/visit  - вход на сайт: ежедневный бонус, «Первый шаг», серии входов
 *  POST /api/activity/view   - открыт материал: очки за первое открытие и значки исследования
 */
@RestController
@RequestMapping("/api")
public class AwardController {

    private final AwardService awardService;
    private final CurrentUserService currentUser;

    public AwardController(AwardService awardService, CurrentUserService currentUser) {
        this.awardService = awardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/awards")
    public AwardsDto awards(Authentication authentication) {
        return awardService.awards(currentUser.require(authentication).getUserId());
    }

    @PostMapping("/activity/visit")
    public RewardEventDto visit(Authentication authentication) {
        return awardService.visit(currentUser.require(authentication).getUserId());
    }

    @PostMapping("/activity/view")
    public RewardEventDto view(@Valid @RequestBody ViewRequest request, Authentication authentication) {
        return awardService.view(currentUser.require(authentication).getUserId(), request.type(), request.key());
    }
}

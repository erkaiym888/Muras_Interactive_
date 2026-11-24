package kg.murasinteractive.rating.controller;

import kg.murasinteractive.rating.dto.RatingDto;
import kg.murasinteractive.rating.service.RatingService;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Рейтинг (FR-5.5). Открыт всем: показываются только отображаемые имена, уровни и очки.
 *  GET /api/rating?period=all  - общий рейтинг
 *  GET /api/rating?period=week - рейтинг текущей недели
 */
@RestController
@RequestMapping("/api/rating")
public class RatingController {

    private final RatingService ratingService;
    private final CurrentUserService currentUser;

    public RatingController(RatingService ratingService, CurrentUserService currentUser) {
        this.ratingService = ratingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public RatingDto rating(@RequestParam(defaultValue = "all") String period, Authentication authentication) {
        Long userId = authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                ? null : currentUser.require(authentication).getUserId();
        return ratingService.rating(period, userId);
    }
}

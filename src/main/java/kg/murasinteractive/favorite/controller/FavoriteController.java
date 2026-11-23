package kg.murasinteractive.favorite.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.favorite.dto.FavoriteDto;
import kg.murasinteractive.favorite.dto.FavoriteRequest;
import kg.murasinteractive.favorite.dto.FavoriteStatusDto;
import kg.murasinteractive.favorite.model.FavoriteType;
import kg.murasinteractive.favorite.service.FavoriteService;
import kg.murasinteractive.user.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Избранное вошедшего пользователя (доступно только после входа).
 *  GET    /api/favorites                          - список
 *  GET    /api/favorites/status?itemType=&itemId= - в избранном ли материал
 *  POST   /api/favorites                          - добавить {itemType, itemId}
 *  DELETE /api/favorites?itemType=&itemId=        - убрать
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserService currentUser;

    public FavoriteController(FavoriteService favoriteService, CurrentUserService currentUser) {
        this.favoriteService = favoriteService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<FavoriteDto> list(Authentication authentication) {
        return favoriteService.list(currentUser.require(authentication));
    }

    @GetMapping("/status")
    public FavoriteStatusDto status(@RequestParam FavoriteType itemType, @RequestParam Long itemId,
                                    Authentication authentication) {
        return new FavoriteStatusDto(favoriteService.isFavorite(currentUser.require(authentication), itemType, itemId));
    }

    @PostMapping
    public ResponseEntity<Void> add(@Valid @RequestBody FavoriteRequest request, Authentication authentication) {
        favoriteService.add(currentUser.require(authentication), request.itemType(), request.itemId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@RequestParam FavoriteType itemType, @RequestParam Long itemId,
                                       Authentication authentication) {
        favoriteService.remove(currentUser.require(authentication), itemType, itemId);
        return ResponseEntity.noContent().build();
    }
}

package kg.murasinteractive.cabinet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kg.murasinteractive.cabinet.dto.ProfileRequests.AvatarRequest;
import kg.murasinteractive.cabinet.dto.ProfileRequests.EmailRequest;
import kg.murasinteractive.cabinet.dto.ProfileRequests.NameRequest;
import kg.murasinteractive.cabinet.service.ProfileService;
import kg.murasinteractive.user.dto.UserDto;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import kg.murasinteractive.user.service.CurrentUserService;
import kg.murasinteractive.user.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Редактирование профиля (FR-6.2), только для вошедшего пользователя (/api/cabinet/**).
 *  PATCH  /api/cabinet/profile/name    - никнейм
 *  PATCH  /api/cabinet/profile/email   - почта (с текущим паролем)
 *  PUT    /api/cabinet/profile/avatar  - аватар (data URL картинки)
 *  DELETE /api/cabinet/profile/avatar  - убрать аватар
 *  GET    /api/users/{id}/avatar       - сама картинка (открыто: аватар виден в рейтинге и меню)
 * Все изменения возвращают обновлённого пользователя - фронтенд сразу обновляет меню и кабинет.
 */
@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUser;
    private final UserAccountRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public ProfileController(ProfileService profileService, CurrentUserService currentUser,
                             UserAccountRepository userRepository, UserDetailsService userDetailsService) {
        this.profileService = profileService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    @PatchMapping("/api/cabinet/profile/name")
    public UserDto rename(@Valid @RequestBody NameRequest request, Authentication authentication) {
        return UserService.toDto(profileService.rename(currentUser.require(authentication), request.displayName()));
    }

    @PatchMapping("/api/cabinet/profile/email")
    public UserDto changeEmail(@Valid @RequestBody EmailRequest request, Authentication authentication,
                               HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserAccount user = profileService.changeEmail(currentUser.require(authentication),
                request.email(), request.currentPassword());
        // Вход привязан к почте: обновляем сессию, иначе со следующим запросом пользователь «потеряется»
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);
        return UserService.toDto(user);
    }

    @PutMapping("/api/cabinet/profile/avatar")
    public UserDto setAvatar(@Valid @RequestBody AvatarRequest request, Authentication authentication) {
        return UserService.toDto(profileService.setAvatar(currentUser.require(authentication), request.dataUrl()));
    }

    @DeleteMapping("/api/cabinet/profile/avatar")
    public UserDto removeAvatar(Authentication authentication) {
        return UserService.toDto(profileService.removeAvatar(currentUser.require(authentication)));
    }

    /** Адрес картинки меняется при каждой смене аватара (?v=...), поэтому её можно кэшировать надолго. */
    @GetMapping("/api/users/{userId}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getAvatar() != null)
                .map(u -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(u.getAvatarType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(u.getAvatar()))
                .orElse(ResponseEntity.notFound().build());
    }
}

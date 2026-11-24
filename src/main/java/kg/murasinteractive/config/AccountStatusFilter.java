package kg.murasinteractive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Решения администратора действуют сразу, без повторного входа (FR-7.3).
 * Роль в сессии запоминается при входе, поэтому на каждом запросе вошедшего пользователя
 * сверяемся с базой: заблокированного - выходим из сессии (дальше он гость),
 * при смене роли - подменяем права в текущем запросе.
 * Не @Component: иначе Spring Boot зарегистрирует фильтр второй раз вне цепочки безопасности.
 */
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserAccountRepository userRepository;

    public AccountStatusFilter(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            UserAccount user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user == null || user.isBlocked()) {
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
            } else {
                String expected = "ROLE_" + user.getRole().getRoleName();
                boolean same = auth.getAuthorities().size() == 1
                        && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).allMatch(expected::equals);
                if (!same) {
                    var updated = UsernamePasswordAuthenticationToken.authenticated(
                            auth.getPrincipal(), null, List.of(new SimpleGrantedAuthority(expected)));
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(updated);
                    SecurityContextHolder.setContext(context);
                }
            }
        }
        chain.doFilter(request, response);
    }
}

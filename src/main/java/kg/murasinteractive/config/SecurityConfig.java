package kg.murasinteractive.config;

import jakarta.servlet.http.HttpServletResponse;
import kg.murasinteractive.user.model.UserRole;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Безопасность (NFR-6): пароли хранятся как BCrypt-хэш, вход по сессии (cookie),
 * все изменяющие запросы защищены CSRF-токеном. Каталог открыт и для гостей (п. 4.1 ТЗ);
 * закрытые разделы (викторины, кабинет) добавятся по мере появления.
 * Викторины, личный кабинет и избранное (/api/quizzes, /api/cabinet, /api/favorites) - только после входа,
 * гость получает 401.
 * Управление содержимым /api/editor/** - только для ролей EDITOR и ADMIN (FR-7.1):
 * гость получает 401, обычный пользователь - 403.
 * Пользователи и роли /api/admin/** - только ADMIN (FR-7.3); блокировка и смена роли действуют сразу (AccountStatusFilter).
 * Вход и выход - стандартные механизмы Spring Security, но с ответами в виде кодов, а не страниц.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UserAccountRepository userRepository) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/editor/**").hasAnyRole(UserRole.EDITOR, UserRole.ADMIN)
                        .requestMatchers("/api/quizzes/**", "/api/cabinet/**", "/api/favorites/**",
                                "/api/awards/**", "/api/activity/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole(UserRole.ADMIN)
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/api/auth/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof DisabledException) {
                                // Заблокированный администратором (FR-7.3) - понятное сообщение вместо «неверный пароль»
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"Аккаунт заблокирован администратором\"}");
                            } else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        }))
                .addFilterBefore(new AccountStatusFilter(userRepository), AuthorizationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN)));
        return http.build();
    }
}

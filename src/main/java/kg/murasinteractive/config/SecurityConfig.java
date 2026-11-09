package kg.murasinteractive.config;

import jakarta.servlet.http.HttpServletResponse;
import kg.murasinteractive.user.model.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Безопасность (NFR-6): пароли хранятся как BCrypt-хэш, вход по сессии (cookie),
 * все изменяющие запросы защищены CSRF-токеном. Каталог открыт и для гостей (п. 4.1 ТЗ);
 * закрытые разделы (викторины, кабинет) добавятся по мере появления.
 * Управление содержимым /api/editor/** - только для ролей EDITOR и ADMIN (FR-7.1):
 * гость получает 401, обычный пользователь - 403.
 * Вход и выход - стандартные механизмы Spring Security, но с ответами в виде кодов, а не страниц.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/editor/**").hasAnyRole(UserRole.EDITOR, UserRole.ADMIN)
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/api/auth/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
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

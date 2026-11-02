package kg.murasinteractive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Временная настройка: пока нет регистрации и входа, все страницы открыты.
 * В шаге FR-6.1 здесь появятся форма входа, выход и PasswordEncoder (BCrypt).
 * Без этого класса Spring Security закрыл бы весь сайт паролем.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

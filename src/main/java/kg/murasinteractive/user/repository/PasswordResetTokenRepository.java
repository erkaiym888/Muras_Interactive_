package kg.murasinteractive.user.repository;

import kg.murasinteractive.user.model.PasswordResetToken;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Удаляет старые неиспользованные ссылки пользователя, чтобы действовала только последняя. */
    void deleteByUserAndUsedAtIsNull(UserAccount user);
}

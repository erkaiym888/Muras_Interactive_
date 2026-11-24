package kg.murasinteractive.awards.repository;

import kg.murasinteractive.awards.model.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    List<UserLevel> findAllByOrderByLevelNumberAsc();

    Optional<UserLevel> findByLevelNumber(int levelNumber);
}

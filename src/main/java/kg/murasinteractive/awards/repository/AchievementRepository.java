package kg.murasinteractive.awards.repository;

import kg.murasinteractive.awards.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByAchievementCode(String achievementCode);

    List<Achievement> findAllByOrderBySortOrderAsc();
}

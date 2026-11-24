package kg.murasinteractive.awards.repository;

import kg.murasinteractive.awards.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    boolean existsByUser_UserIdAndAchievement_AchievementCode(Long userId, String achievementCode);

    List<UserAchievement> findByUser_UserIdOrderByAwardedAtDesc(Long userId);
}

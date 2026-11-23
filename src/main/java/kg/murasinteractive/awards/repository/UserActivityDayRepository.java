package kg.murasinteractive.awards.repository;

import kg.murasinteractive.awards.model.UserActivityDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserActivityDayRepository extends JpaRepository<UserActivityDay, Long> {

    boolean existsByUser_UserIdAndActivityDate(Long userId, LocalDate activityDate);

    List<UserActivityDay> findByUser_UserIdOrderByActivityDateDesc(Long userId);
}

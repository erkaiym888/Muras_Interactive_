package kg.murasinteractive.awards.service;

import kg.murasinteractive.awards.dto.LevelDto;
import kg.murasinteractive.awards.model.UserLevel;
import kg.murasinteractive.awards.repository.UserLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Уровни (Приложение Б.2): уровень определяется суммой очков total_points. */
@Service
public class LevelService {

    /** С этого уровня («Акын») открываются викторины повышенной сложности. */
    public static final int HARD_QUIZ_LEVEL = 5;

    private final UserLevelRepository levelRepository;

    public LevelService(UserLevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }

    @Transactional(readOnly = true)
    public List<LevelDto> levels() {
        return levelRepository.findAllByOrderByLevelNumberAsc().stream().map(LevelService::toDto).toList();
    }

    /** Наибольший уровень, порог которого не больше суммы очков. */
    @Transactional(readOnly = true)
    public LevelDto levelFor(int points) {
        List<LevelDto> levels = levels();
        LevelDto current = levels.isEmpty() ? new LevelDto(1, "Конок (Гость)", 0, null) : levels.get(0);
        for (LevelDto level : levels) {
            if (points >= level.minPoints()) {
                current = level;
            }
        }
        return current;
    }

    /** Следующий уровень или null, если достигнут высший. */
    @Transactional(readOnly = true)
    public LevelDto nextLevel(int points) {
        return levels().stream().filter(l -> l.minPoints() > points).findFirst().orElse(null);
    }

    public static LevelDto toDto(UserLevel level) {
        return new LevelDto(level.getLevelNumber(), level.getLevelTitle(), level.getMinPoints(),
                level.getRewardDescription());
    }
}

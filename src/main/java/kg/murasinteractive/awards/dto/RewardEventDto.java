package kg.murasinteractive.awards.dto;

import kg.murasinteractive.quiz.dto.PointLineDto;

import java.util.List;

/**
 * Что пользователь только что получил: очки с расшифровкой, новые значки и новый уровень (если был переход).
 * Возвращается после входа на сайт, открытия материала и прохождения викторины.
 */
public record RewardEventDto(List<PointLineDto> points, int pointsEarned, int totalPoints,
                             List<AchievementDto> newAchievements, LevelDto levelUp) {
}

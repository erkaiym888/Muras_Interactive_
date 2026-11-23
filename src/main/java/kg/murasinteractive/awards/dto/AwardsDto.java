package kg.murasinteractive.awards.dto;

import java.util.List;

/**
 * Награды пользователя (FR-5.3): текущий и следующий уровень, сколько очков осталось,
 * серия ежедневных входов, все 8 уровней и все 15 значков с отметкой «получен».
 */
public record AwardsDto(int totalPoints, LevelDto level, LevelDto nextLevel, int pointsToNext, int progressPercent,
                        int loginStreak, List<LevelDto> levels, List<AchievementDto> achievements) {
}

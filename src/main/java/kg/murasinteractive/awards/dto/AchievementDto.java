package kg.murasinteractive.awards.dto;

import java.time.Instant;

/** Значок: earned - получен ли пользователем, awardedAt - когда. */
public record AchievementDto(String code, String title, String group, String condition, int bonus, String icon,
                             boolean earned, Instant awardedAt) {
}

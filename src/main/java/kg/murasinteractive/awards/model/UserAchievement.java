package kg.murasinteractive.awards.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.Instant;

/** Полученный пользователем значок (user_achievement). Каждый значок выдаётся один раз. */
@Entity
@Table(name = "user_achievement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_id"}))
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_achievement_id")
    private Long userAchievementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt = Instant.now();

    public UserAchievement() {}

    public UserAchievement(UserAccount user, Achievement achievement) {
        this.user = user;
        this.achievement = achievement;
    }

    public Achievement getAchievement() { return achievement; }
    public Instant getAwardedAt() { return awardedAt; }
}

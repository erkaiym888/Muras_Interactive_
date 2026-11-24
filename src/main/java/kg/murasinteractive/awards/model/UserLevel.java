package kg.murasinteractive.awards.model;

import jakarta.persistence.*;

/** Уровень пользователя (Приложение Б.2): определяется суммой очков total_points. */
@Entity
@Table(name = "user_level")
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_id")
    private Long levelId;

    @Column(name = "level_number", nullable = false, unique = true)
    private int levelNumber;

    @Column(name = "level_title", nullable = false, length = 100)
    private String levelTitle;

    @Column(name = "min_points", nullable = false)
    private int minPoints;

    /** Награда за переход на уровень: рамка профиля, доступ к сложным викторинам и т. д. */
    @Column(name = "reward_description", length = 300)
    private String rewardDescription;

    public UserLevel() {}

    public UserLevel(int levelNumber, String levelTitle, int minPoints, String rewardDescription) {
        this.levelNumber = levelNumber;
        this.levelTitle = levelTitle;
        this.minPoints = minPoints;
        this.rewardDescription = rewardDescription;
    }

    public Long getLevelId() { return levelId; }
    public int getLevelNumber() { return levelNumber; }
    public String getLevelTitle() { return levelTitle; }
    public void setLevelTitle(String levelTitle) { this.levelTitle = levelTitle; }
    public int getMinPoints() { return minPoints; }
    public void setMinPoints(int minPoints) { this.minPoints = minPoints; }
    public String getRewardDescription() { return rewardDescription; }
    public void setRewardDescription(String rewardDescription) { this.rewardDescription = rewardDescription; }
}

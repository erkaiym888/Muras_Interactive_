package kg.murasinteractive.awards.model;

import jakarta.persistence.*;

/** Значок (Приложение Б.3): код, название, условие, бонус очков и иконка. */
@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "achievement_id")
    private Long achievementId;

    @Column(name = "achievement_code", nullable = false, unique = true, length = 50)
    private String achievementCode;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** Группа: Исследование, Викторины, Постоянство, Рейтинг. */
    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "condition_description", nullable = false, length = 300)
    private String conditionDescription;

    @Column(name = "points_bonus", nullable = false)
    private int pointsBonus;

    @Column(name = "icon_path", length = 200)
    private String iconPath;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Achievement() {}

    public Achievement(String code, String title, String groupName, String condition, int bonus, int sortOrder) {
        this.achievementCode = code;
        this.title = title;
        this.groupName = groupName;
        this.conditionDescription = condition;
        this.pointsBonus = bonus;
        this.iconPath = "badges/" + code.toLowerCase().replace('_', '-') + ".svg";
        this.sortOrder = sortOrder;
    }

    public Long getAchievementId() { return achievementId; }
    public String getAchievementCode() { return achievementCode; }
    public String getTitle() { return title; }
    public String getGroupName() { return groupName; }
    public String getConditionDescription() { return conditionDescription; }
    public int getPointsBonus() { return pointsBonus; }
    public String getIconPath() { return iconPath; }
    public int getSortOrder() { return sortOrder; }
}

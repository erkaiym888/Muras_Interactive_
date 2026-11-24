package kg.murasinteractive.awards.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.LocalDate;

/** День, когда пользователь заходил на сайт (по Бишкеку): ежедневный бонус и серии входов. */
@Entity
@Table(name = "user_activity_day",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"}))
public class UserActivityDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_day_id")
    private Long activityDayId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    public UserActivityDay() {}

    public UserActivityDay(UserAccount user, LocalDate activityDate) {
        this.user = user;
        this.activityDate = activityDate;
    }

    public LocalDate getActivityDate() { return activityDate; }
}

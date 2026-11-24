package kg.murasinteractive.awards.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.Instant;

/**
 * Что пользователь уже открывал: элемент юрты, карточку обряда, героя, произведения,
 * результат поиска. Нужно для очков «первое открытие / прочтение» и значков исследования.
 */
@Entity
@Table(name = "content_view",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_type", "item_key"}))
public class ContentView {

    public static final String YURT_PART = "YURT_PART";
    public static final String RITE = "RITE";
    public static final String HERO = "HERO";
    public static final String WORK = "WORK";
    public static final String SEARCH = "SEARCH";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_view_id")
    private Long contentViewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_key", nullable = false, length = 200)
    private String itemKey;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();

    public ContentView() {}

    public ContentView(UserAccount user, String itemType, String itemKey) {
        this.user = user;
        this.itemType = itemType;
        this.itemKey = itemKey;
    }

    public String getItemType() { return itemType; }
    public String getItemKey() { return itemKey; }
    public Instant getViewedAt() { return viewedAt; }
}

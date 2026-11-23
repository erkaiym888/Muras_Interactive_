package kg.murasinteractive.favorite.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.Instant;

/** Избранный материал пользователя (таблица favorite). */
@Entity
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_type", "item_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private FavoriteType itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Favorite() {}

    public Favorite(UserAccount user, FavoriteType itemType, Long itemId) {
        this.user = user;
        this.itemType = itemType;
        this.itemId = itemId;
    }

    public Long getFavoriteId() { return favoriteId; }
    public UserAccount getUser() { return user; }
    public FavoriteType getItemType() { return itemType; }
    public Long getItemId() { return itemId; }
    public Instant getCreatedAt() { return createdAt; }
}

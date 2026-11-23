package kg.murasinteractive.points.model;

import jakarta.persistence.*;
import kg.murasinteractive.user.model.UserAccount;

import java.time.Instant;

/**
 * Начисление очков (таблица point_transaction по ТЗ). Каждое начисление - отдельная строка,
 * по ним строятся общий и недельный рейтинги (FR-5.5) и видно, за что получены очки.
 */
@Entity
@Table(name = "point_transaction")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** Источник: QUIZ_ANSWERS, QUIZ_STREAK, QUIZ_COMPLETE, QUIZ_RESULT, QUIZ_FIRST_PASS и т. д. */
    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    /** Запись-источник, например id попытки викторины. */
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "points_delta", nullable = false)
    private int pointsDelta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public PointTransaction() {}

    public PointTransaction(UserAccount user, String sourceType, Long sourceId, int pointsDelta) {
        this.user = user;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.pointsDelta = pointsDelta;
    }

    public Long getTransactionId() { return transactionId; }
    public UserAccount getUser() { return user; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public int getPointsDelta() { return pointsDelta; }
    public Instant getCreatedAt() { return createdAt; }
}

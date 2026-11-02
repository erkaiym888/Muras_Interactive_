package kg.murasinteractive.user.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Учётная запись (таблица user_account по ТЗ). Пароль хранится только в виде BCrypt-хэша. */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRole role;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public Instant getRegisteredAt() { return registeredAt; }
}

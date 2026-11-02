package kg.murasinteractive.user.model;

import jakarta.persistence.*;

/** Роль пользователя (таблица user_role). Гость - это просто не вошедший посетитель, в базе его нет. */
@Entity
@Table(name = "user_role")
public class UserRole {

    public static final String USER = "USER";
    public static final String EDITOR = "EDITOR";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    public UserRole() {}

    public UserRole(String roleName) {
        this.roleName = roleName;
    }

    public Long getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}

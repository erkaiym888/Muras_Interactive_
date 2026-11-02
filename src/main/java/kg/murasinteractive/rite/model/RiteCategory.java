package kg.murasinteractive.rite.model;

import jakarta.persistence.*;

/** Категория обрядов (таблица rite_category). Список хранится в БД, а не в коде (FR-2.1). */
@Entity
@Table(name = "rite_category")
public class RiteCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rite_category_id")
    private Long riteCategoryId;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    private String categoryName;

    public RiteCategory() {}

    public RiteCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getRiteCategoryId() { return riteCategoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}

package kg.murasinteractive.yurt.model;

import jakarta.persistence.*;

/**
 * Элемент юрты (таблица yurt_element, п. 4.4 ТЗ). Подробная карточка и координаты на схеме
 * живут во фронтенде (design/src/yurt.ts); в базе - то, что нужно для поиска (FR-4.1) и связей с обрядами (FR-3.3).
 */
@Entity
@Table(name = "yurt_element")
public class YurtElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yurt_element_id")
    private Long yurtElementId;

    /** Ключ элемента на схеме: tunduk, kerege, uuk... */
    @Column(name = "element_key", nullable = false, unique = true, length = 40)
    private String elementKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Группа: каркас, покрытие, связки, убранство, вход. */
    @Column(name = "element_group", length = 40)
    private String group;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Положение на схеме; пока схема берёт координаты из yurt.ts, поле на будущее. */
    @Column(name = "map_coordinates", length = 100)
    private String mapCoordinates;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public YurtElement() {}

    public YurtElement(String elementKey, String name, String group, String description, int sortOrder) {
        this.elementKey = elementKey;
        this.name = name;
        this.group = group;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public Long getYurtElementId() { return yurtElementId; }
    public String getElementKey() { return elementKey; }
    public String getName() { return name; }
    public String getGroup() { return group; }
    public String getDescription() { return description; }
    public String getMapCoordinates() { return mapCoordinates; }
    public int getSortOrder() { return sortOrder; }
}

package kg.murasinteractive.i18n.model;

import jakarta.persistence.*;

/**
 * Перевод одного текстового поля материала (FR-8.1, таблица content_translation из п. 4.4 ТЗ).
 * Основной (русский) текст остаётся в таблице самого материала, здесь - только другие языки.
 * Пример: entity_type=RITE, entity_id=3, language_code=ky, field_name=title, text_value="Бешик той".
 */
@Entity
@Table(name = "content_translation", uniqueConstraints = @UniqueConstraint(
        name = "uk_content_translation", columnNames = {"entity_type", "entity_id", "language_code", "field_name"}),
        indexes = @Index(name = "ix_content_translation_lookup", columnList = "entity_type, language_code, entity_id"))
public class ContentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "field_name", nullable = false, length = 60)
    private String fieldName;

    @Column(name = "text_value", nullable = false, columnDefinition = "text")
    private String textValue;

    public ContentTranslation() {}

    public ContentTranslation(String entityType, Long entityId, String languageCode, String fieldName, String textValue) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.languageCode = languageCode;
        this.fieldName = fieldName;
        this.textValue = textValue;
    }

    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getLanguageCode() { return languageCode; }
    public String getFieldName() { return fieldName; }
    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
}

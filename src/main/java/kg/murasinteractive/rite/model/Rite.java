package kg.murasinteractive.rite.model;

import jakarta.persistence.*;
import kg.murasinteractive.media.model.MediaFile;

import java.util.ArrayList;
import java.util.List;

/** Обряд или традиция (таблица rite). Поля карточки по FR-2.2. */
@Entity
@Table(name = "rite")
public class Rite {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rite_id")
    private Long riteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rite_category_id", nullable = false)
    private RiteCategory riteCategory;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Короткая аннотация для карточки в каталоге. */
    @Column(name = "summary", length = 500)
    private String summary;

    /** Полное описание (абзацы разделяются пустой строкой). */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Символика обряда. */
    @Column(name = "symbolism", columnDefinition = "TEXT")
    private String symbolism;

    /** Источники (книги, статьи). */
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    /** DRAFT / PUBLISHED / HIDDEN. Пока показываем только PUBLISHED. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_DRAFT;

    @OneToMany(mappedBy = "rite", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<RiteStep> steps = new ArrayList<>();

    /** Изображения обряда (связь многие-ко-многим через таблицу rite_media). */
    @ManyToMany
    @JoinTable(name = "rite_media",
            joinColumns = @JoinColumn(name = "rite_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    private List<MediaFile> media = new ArrayList<>();

    public Rite() {}

    /** Добавляет этап с нужным номером. */
    public void addStep(int order, String title, String text) {
        RiteStep step = new RiteStep();
        step.setRite(this);
        step.setStepOrder(order);
        step.setStepTitle(title);
        step.setStepText(text);
        steps.add(step);
    }

    public Long getRiteId() { return riteId; }
    public RiteCategory getRiteCategory() { return riteCategory; }
    public void setRiteCategory(RiteCategory riteCategory) { this.riteCategory = riteCategory; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSymbolism() { return symbolism; }
    public void setSymbolism(String symbolism) { this.symbolism = symbolism; }
    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<RiteStep> getSteps() { return steps; }
    public List<MediaFile> getMedia() { return media; }
}

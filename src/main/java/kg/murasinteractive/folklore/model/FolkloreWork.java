package kg.murasinteractive.folklore.model;

import jakarta.persistence.*;
import kg.murasinteractive.media.model.MediaFile;

import java.util.ArrayList;
import java.util.List;

/** Произведение: эпос, сказка, легенда, пословицы (таблица folklore_work по ТЗ). */
@Entity
@Table(name = "folklore_work")
public class FolkloreWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_id")
    private Long workId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_type_id", nullable = false)
    private FolkloreType workType;

    /** Краткое содержание. */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Герои произведения: обратная сторона связи hero_work. */
    @ManyToMany(mappedBy = "works")
    @OrderBy("heroId ASC")
    private List<FolkloreHero> heroes = new ArrayList<>();

    /** Иллюстрации (связующая таблица work_media). */
    @ManyToMany
    @JoinTable(name = "work_media",
            joinColumns = @JoinColumn(name = "work_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    @OrderBy("mediaId ASC")
    private List<MediaFile> media = new ArrayList<>();

    public FolkloreWork() {}

    public Long getWorkId() { return workId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public FolkloreType getWorkType() { return workType; }
    public void setWorkType(FolkloreType workType) { this.workType = workType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<FolkloreHero> getHeroes() { return heroes; }
    public List<MediaFile> getMedia() { return media; }
}

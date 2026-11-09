package kg.murasinteractive.folklore.model;

import jakarta.persistence.*;
import kg.murasinteractive.media.model.MediaFile;

import java.util.ArrayList;
import java.util.List;

/** Герой или персонаж (таблица folklore_hero по ТЗ). Тип берётся из справочника folklore_type. */
@Entity
@Table(name = "folklore_hero")
public class FolkloreHero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hero_id")
    private Long heroId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_type_id", nullable = false)
    private FolkloreType heroType;

    /** Краткое описание. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Связанные произведения (связующая таблица hero_work). */
    @ManyToMany
    @JoinTable(name = "hero_work",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "work_id"))
    @OrderBy("workId ASC")
    private List<FolkloreWork> works = new ArrayList<>();

    /** Иллюстрации (связующая таблица hero_media). */
    @ManyToMany
    @JoinTable(name = "hero_media",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    @OrderBy("mediaId ASC")
    private List<MediaFile> media = new ArrayList<>();

    public FolkloreHero() {}

    public Long getHeroId() { return heroId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FolkloreType getHeroType() { return heroType; }
    public void setHeroType(FolkloreType heroType) { this.heroType = heroType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<FolkloreWork> getWorks() { return works; }
    public List<MediaFile> getMedia() { return media; }
}

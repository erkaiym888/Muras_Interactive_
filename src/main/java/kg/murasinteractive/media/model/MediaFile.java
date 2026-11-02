package kg.murasinteractive.media.model;

import jakarta.persistence.*;

/** Файл-иллюстрация (таблица media_file). Общая для обрядов, героев, элементов юрты. */
@Entity
@Table(name = "media_file")
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    /** Путь внутри static, например /images/rites/beshik.jpg */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType = "IMAGE";

    /** Подпись; используется как alt-текст картинки (NFR-5). */
    @Column(name = "caption", length = 500)
    private String caption;

    public MediaFile() {}

    public MediaFile(String filePath, String mediaType, String caption) {
        this.filePath = filePath;
        this.mediaType = mediaType;
        this.caption = caption;
    }

    public Long getMediaId() { return mediaId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}

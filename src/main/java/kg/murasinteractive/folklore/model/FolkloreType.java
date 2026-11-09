package kg.murasinteractive.folklore.model;

import jakarta.persistence.*;

/** Справочник типов фольклора: герои, эпос, сказки, легенды, пословицы и поговорки (FR-3.1). */
@Entity
@Table(name = "folklore_type")
public class FolkloreType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folklore_type_id")
    private Long folkloreTypeId;

    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    private String typeName;

    public FolkloreType() {}

    public FolkloreType(String typeName) {
        this.typeName = typeName;
    }

    public Long getFolkloreTypeId() { return folkloreTypeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
}

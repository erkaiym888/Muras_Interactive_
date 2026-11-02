package kg.murasinteractive.media.repository;

import kg.murasinteractive.media.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
}

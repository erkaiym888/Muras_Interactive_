package kg.murasinteractive.i18n.repository;

import kg.murasinteractive.i18n.model.ContentTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, Long> {

    List<ContentTranslation> findByEntityTypeAndLanguageCodeAndEntityIdIn(String entityType, String languageCode,
                                                                         Collection<Long> entityIds);

    List<ContentTranslation> findByEntityTypeAndLanguageCode(String entityType, String languageCode);

    Optional<ContentTranslation> findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
            String entityType, Long entityId, String languageCode, String fieldName);

    long countByLanguageCode(String languageCode);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}

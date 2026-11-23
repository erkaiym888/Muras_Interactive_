package kg.murasinteractive.i18n.repository;

import kg.murasinteractive.i18n.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanguageRepository extends JpaRepository<Language, String> {

    List<Language> findAllByOrderBySortOrderAsc();

    List<Language> findByEnabledTrueOrderBySortOrderAsc();
}

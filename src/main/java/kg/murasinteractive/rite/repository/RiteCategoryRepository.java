package kg.murasinteractive.rite.repository;

import kg.murasinteractive.rite.model.RiteCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiteCategoryRepository extends JpaRepository<RiteCategory, Long> {

    Optional<RiteCategory> findByCategoryName(String categoryName);

    List<RiteCategory> findAllByOrderByRiteCategoryIdAsc();
}

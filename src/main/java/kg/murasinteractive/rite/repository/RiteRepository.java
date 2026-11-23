package kg.murasinteractive.rite.repository;

import kg.murasinteractive.rite.model.Rite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiteRepository extends JpaRepository<Rite, Long> {

    List<Rite> findByStatusOrderByRiteIdAsc(String status);

    Optional<Rite> findByRiteIdAndStatus(Long riteId, String status);

    /** Все обряды в любом статусе - для редактора контента (FR-7.1). */
    List<Rite> findAllByOrderByRiteIdDesc();

    /** Обратные связи (FR-3.3): опубликованные обряды, связанные с героем, произведением или элементом юрты. */
    List<Rite> findByHeroes_HeroIdAndStatusOrderByTitleAsc(Long heroId, String status);

    List<Rite> findByWorks_WorkIdAndStatusOrderByTitleAsc(Long workId, String status);

    List<Rite> findByYurtElements_ElementKeyAndStatusOrderByTitleAsc(String elementKey, String status);

    boolean existsByRiteCategory_RiteCategoryId(Long riteCategoryId);

    long countByRiteCategory_RiteCategoryId(Long riteCategoryId);
}

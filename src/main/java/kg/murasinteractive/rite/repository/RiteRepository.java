package kg.murasinteractive.rite.repository;

import kg.murasinteractive.rite.model.Rite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiteRepository extends JpaRepository<Rite, Long> {

    List<Rite> findByStatusOrderByRiteIdAsc(String status);

    Optional<Rite> findByRiteIdAndStatus(Long riteId, String status);
}

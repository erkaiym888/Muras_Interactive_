package kg.murasinteractive.points.repository;

import kg.murasinteractive.points.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /** Начисления за одно прохождение викторины - для расшифровки очков в истории. */
    List<PointTransaction> findBySourceIdAndSourceTypeStartingWithOrderByTransactionIdAsc(Long sourceId, String prefix);
}

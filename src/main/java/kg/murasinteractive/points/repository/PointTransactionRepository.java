package kg.murasinteractive.points.repository;

import kg.murasinteractive.points.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}

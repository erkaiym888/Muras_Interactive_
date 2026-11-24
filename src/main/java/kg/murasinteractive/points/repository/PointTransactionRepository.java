package kg.murasinteractive.points.repository;

import kg.murasinteractive.points.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /** Начисления за одно прохождение викторины - для расшифровки очков в истории. */
    List<PointTransaction> findBySourceIdAndSourceTypeStartingWithOrderByTransactionIdAsc(Long sourceId, String prefix);

    /** Сумма очков каждого пользователя за период [from, to) - для недельного рейтинга (FR-5.5). */
    @Query("select t.user.userId, sum(t.pointsDelta) from PointTransaction t "
            + "where t.createdAt >= :from and t.createdAt < :to group by t.user.userId")
    List<Object[]> sumByUserBetween(@Param("from") Instant from, @Param("to") Instant to);
}

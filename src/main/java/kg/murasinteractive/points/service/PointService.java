package kg.murasinteractive.points.service;

import kg.murasinteractive.points.model.PointTransaction;
import kg.murasinteractive.points.repository.PointTransactionRepository;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Начисляет очки: пишет строку в point_transaction и увеличивает total_points пользователя. */
@Service
public class PointService {

    private final PointTransactionRepository transactionRepository;

    public PointService(PointTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void award(UserAccount user, String sourceType, Long sourceId, int points) {
        if (points <= 0) {
            return;
        }
        transactionRepository.save(new PointTransaction(user, sourceType, sourceId, points));
        user.setTotalPoints(user.getTotalPoints() + points);
    }
}

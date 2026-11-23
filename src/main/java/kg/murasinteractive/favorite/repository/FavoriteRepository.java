package kg.murasinteractive.favorite.repository;

import kg.murasinteractive.favorite.model.Favorite;
import kg.murasinteractive.favorite.model.FavoriteType;
import kg.murasinteractive.user.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserOrderByCreatedAtDesc(UserAccount user);

    boolean existsByUserAndItemTypeAndItemId(UserAccount user, FavoriteType itemType, Long itemId);

    void deleteByUserAndItemTypeAndItemId(UserAccount user, FavoriteType itemType, Long itemId);
}

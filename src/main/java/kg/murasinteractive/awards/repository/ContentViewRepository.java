package kg.murasinteractive.awards.repository;

import kg.murasinteractive.awards.model.ContentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ContentViewRepository extends JpaRepository<ContentView, Long> {

    boolean existsByUser_UserIdAndItemTypeAndItemKey(Long userId, String itemType, String itemKey);

    long countByUser_UserIdAndItemTypeIn(Long userId, Collection<String> itemTypes);

    long countByUser_UserIdAndItemTypeInAndViewedAtGreaterThanEqual(Long userId, Collection<String> itemTypes, Instant from);

    List<ContentView> findByUser_UserIdAndItemType(Long userId, String itemType);
}

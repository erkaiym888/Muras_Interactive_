package kg.murasinteractive.yurt.repository;

import kg.murasinteractive.yurt.model.YurtElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface YurtElementRepository extends JpaRepository<YurtElement, Long> {

    Optional<YurtElement> findByElementKey(String elementKey);

    List<YurtElement> findAllByOrderBySortOrderAsc();

    List<YurtElement> findByElementKeyIn(Collection<String> keys);
}

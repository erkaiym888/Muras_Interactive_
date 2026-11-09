package kg.murasinteractive.folklore.repository;

import kg.murasinteractive.folklore.model.FolkloreType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolkloreTypeRepository extends JpaRepository<FolkloreType, Long> {

    Optional<FolkloreType> findByTypeName(String typeName);

    List<FolkloreType> findAllByOrderByFolkloreTypeIdAsc();
}

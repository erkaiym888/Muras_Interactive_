package kg.murasinteractive.folklore.repository;

import kg.murasinteractive.folklore.model.FolkloreWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolkloreWorkRepository extends JpaRepository<FolkloreWork, Long> {

    List<FolkloreWork> findAllByOrderByWorkIdAsc();
}

package kg.murasinteractive.folklore.repository;

import kg.murasinteractive.folklore.model.FolkloreHero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolkloreHeroRepository extends JpaRepository<FolkloreHero, Long> {

    List<FolkloreHero> findAllByOrderByHeroIdAsc();
}

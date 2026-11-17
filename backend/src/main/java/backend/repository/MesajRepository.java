package backend.repository;

import backend.model.Mesaj;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesajRepository extends JpaRepository<Mesaj, Long> {

    @EntityGraph(attributePaths = "autor") // autorul vine in aceeasi interogare
    List<Mesaj> findByConversatieIdOrderByTrimisLaAscIdAsc(Long conversatieId);
}

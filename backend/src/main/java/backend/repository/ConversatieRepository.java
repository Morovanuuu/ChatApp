package backend.repository;

import backend.model.Conversatie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConversatieRepository extends JpaRepository<Conversatie, Long> {

    /** Conversatiile utilizatorului, cu toti membrii incarcati intr-o singura interogare (fara N+1). */
    @Query("""
            select distinct c from Conversatie c join fetch c.membri
            where c.id in (select c2.id from Conversatie c2 join c2.membri m where m.email = :email)
            order by c.creatLa desc""")
    List<Conversatie> conversatiileLui(String email);

    boolean existsByIdAndMembri_Email(Long id, String email);
}

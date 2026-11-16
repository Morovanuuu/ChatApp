package backend.repository;

import backend.model.Utilizator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilizatorRepository extends JpaRepository<Utilizator, Long> {

    Optional<Utilizator> findByEmailIgnoreCase(String email);
}

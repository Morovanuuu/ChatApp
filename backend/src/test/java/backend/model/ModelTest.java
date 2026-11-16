package backend.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional // rollback dupa fiecare test: baza ramane curata
class ModelTest {

    @PersistenceContext
    EntityManager em;

    private Utilizator dupaEmail(String email) {
        return em.createQuery("select u from Utilizator u where u.email = :e", Utilizator.class)
                .setParameter("e", email)
                .getSingleResult();
    }

    @Test
    void utilizatoriiDeTestExista() {
        assertEquals(Rol.ADMIN, dupaEmail("admin@chatapp.ro").getRol());
        assertEquals(Rol.USER, dupaEmail("ana@chatapp.ro").getRol());
        assertEquals(Rol.USER, dupaEmail("mihai@chatapp.ro").getRol());
    }

    @Test
    void conversatieCuMembriSiMesaj() {
        Utilizator ana = dupaEmail("ana@chatapp.ro");
        Utilizator mihai = dupaEmail("mihai@chatapp.ro");

        Conversatie c = new Conversatie(null);
        c.getMembri().add(ana);
        c.getMembri().add(mihai);
        em.persist(c);
        em.persist(new Mesaj(c, ana, "Salut!"));
        em.flush();
        em.clear();

        long membri = ((Number) em.createNativeQuery(
                        "select count(*) from membru_conversatie where conversatie_id = ?1")
                .setParameter(1, c.getId())
                .getSingleResult()).longValue();
        assertEquals(2, membri);

        Mesaj m = em.createQuery(
                        "select m from Mesaj m join fetch m.autor where m.conversatie.id = :id", Mesaj.class)
                .setParameter("id", c.getId())
                .getSingleResult();
        assertEquals("Salut!", m.getContinut());
        assertEquals("Ana Popescu", m.getAutor().getNume());
    }
}

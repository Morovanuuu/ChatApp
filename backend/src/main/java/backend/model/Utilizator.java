package backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilizator")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // cerut de JPA
public class Utilizator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Setter
    @Column(name = "parola_hash", nullable = false, length = 100)
    private String parolaHash;

    @Setter
    @Column(nullable = false, length = 100)
    private String nume;

    @Enumerated(EnumType.STRING) // salveaza "ADMIN", nu pozitia 0
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "creat_la", nullable = false, updatable = false)
    private LocalDateTime creatLa;

    public Utilizator(String email, String parolaHash, String nume, Rol rol) {
        this.email = email;
        this.parolaHash = parolaHash;
        this.nume = nume;
        this.rol = rol;
    }

    @PrePersist
    void laCreare() {
        creatLa = LocalDateTime.now();
    }
}

package backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mesaj")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mesaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversatie_id")
    private Conversatie conversatie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Utilizator autor;

    @Column(nullable = false, length = 2000)
    private String continut;

    @Column(name = "trimis_la", nullable = false, updatable = false)
    private LocalDateTime trimisLa;

    public Mesaj(Conversatie conversatie, Utilizator autor, String continut) {
        this.conversatie = conversatie;
        this.autor = autor;
        this.continut = continut;
    }

    @PrePersist
    void laTrimitere() {
        trimisLa = LocalDateTime.now();
    }
}

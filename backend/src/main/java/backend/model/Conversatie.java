package backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversatie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversatie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(length = 100)
    private String nume; // null = conversatie privata intre doi utilizatori

    @Column(name = "creat_la", nullable = false, updatable = false)
    private LocalDateTime creatLa;

    // shortcut: intrat_la din membru_conversatie e completat de DB (DEFAULT) si nu e citit in Java;
    // cand e nevoie de el, se inlocuieste cu o entitate MembruConversatie cu @EmbeddedId.
    @ManyToMany
    @JoinTable(name = "membru_conversatie",
            joinColumns = @JoinColumn(name = "conversatie_id"),
            inverseJoinColumns = @JoinColumn(name = "utilizator_id"))
    private Set<Utilizator> membri = new HashSet<>();

    public Conversatie(String nume) {
        this.nume = nume;
    }

    @PrePersist
    void laCreare() {
        creatLa = LocalDateTime.now();
    }
}

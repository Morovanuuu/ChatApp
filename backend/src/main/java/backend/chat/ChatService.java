package backend.chat;

import backend.chat.ChatController.ConversatieDto;
import backend.chat.ChatController.MesajDto;
import backend.chat.ChatController.UtilizatorDto;
import backend.model.Conversatie;
import backend.model.Mesaj;
import backend.model.Utilizator;
import backend.repository.ConversatieRepository;
import backend.repository.MesajRepository;
import backend.repository.UtilizatorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Logica de chat, separata de HTTP ca sa o refoloseasca si WebSocket-ul.
 * "email" e mereu cel din token (utilizatorul logat).
 */
@Service
@Transactional
public class ChatService {

    private final ConversatieRepository conversatii;
    private final MesajRepository mesaje;
    private final UtilizatorRepository utilizatori;

    public ChatService(ConversatieRepository conversatii, MesajRepository mesaje, UtilizatorRepository utilizatori) {
        this.conversatii = conversatii;
        this.mesaje = mesaje;
        this.utilizatori = utilizatori;
    }

    @Transactional(readOnly = true)
    public List<ConversatieDto> conversatiileMele(String email) {
        return conversatii.conversatiileLui(email).stream().map(ChatService::dto).toList();
    }

    /** nume gol = conversatie privata (exact un alt membru); altfel grup. Creatorul intra automat. */
    public ConversatieDto creeaza(String email, String nume, List<String> emailuriMembri) {
        Utilizator eu = utilizator(email);
        Set<Utilizator> membri = new LinkedHashSet<>();
        membri.add(eu);
        for (String e : emailuriMembri) {
            membri.add(utilizatori.findByEmailIgnoreCase(e.trim())
                    .orElseThrow(() -> eroare(HttpStatus.BAD_REQUEST, "Utilizator inexistent: " + e)));
        }
        if (membri.size() < 2) {
            throw eroare(HttpStatus.BAD_REQUEST, "Adaugati cel putin un alt membru");
        }
        boolean privata = nume == null || nume.isBlank();
        if (privata && membri.size() != 2) {
            throw eroare(HttpStatus.BAD_REQUEST, "O conversatie privata are exact doi membri; pentru grup dati un nume");
        }
        // shortcut: doua conversatii private intre aceleasi persoane sunt permise; daca deranjeaza, se intoarce cea existenta
        Conversatie c = new Conversatie(privata ? null : nume.trim());
        c.getMembri().addAll(membri);
        return dto(conversatii.save(c));
    }

    // shortcut: intoarce tot istoricul; cand conversatiile cresc, se trece la paginare (Pageable)
    @Transactional(readOnly = true)
    public List<MesajDto> mesaje(String email, Long conversatieId) {
        verificaMembru(email, conversatieId);
        return mesaje.findByConversatieIdOrderByTrimisLaAscIdAsc(conversatieId).stream().map(ChatService::dto).toList();
    }

    public MesajDto trimite(String email, Long conversatieId, String continut) {
        verificaMembru(email, conversatieId);
        Mesaj m = new Mesaj(conversatii.getReferenceById(conversatieId), utilizator(email), continut.trim());
        return dto(mesaje.save(m));
    }

    /** 404 si cand conversatia exista, dar nu esti membru: nu aflam ce id-uri exista. */
    private void verificaMembru(String email, Long conversatieId) {
        if (!conversatii.existsByIdAndMembri_Email(conversatieId, email)) {
            throw eroare(HttpStatus.NOT_FOUND, "Conversatia nu exista");
        }
    }

    private Utilizator utilizator(String email) {
        // tokenul e valid, dar contul poate fi sters intre timp
        return utilizatori.findByEmailIgnoreCase(email)
                .orElseThrow(() -> eroare(HttpStatus.UNAUTHORIZED, "Contul nu mai exista"));
    }

    private static ResponseStatusException eroare(HttpStatus status, String mesaj) {
        return new ResponseStatusException(status, mesaj);
    }

    private static ConversatieDto dto(Conversatie c) {
        List<UtilizatorDto> membri = c.getMembri().stream()
                .map(u -> new UtilizatorDto(u.getId(), u.getNume(), u.getEmail()))
                .sorted(Comparator.comparing(UtilizatorDto::nume))
                .toList();
        return new ConversatieDto(c.getId(), c.getNume(), membri, c.getCreatLa());
    }

    private static MesajDto dto(Mesaj m) {
        Utilizator a = m.getAutor();
        return new MesajDto(m.getId(), new UtilizatorDto(a.getId(), a.getNume(), a.getEmail()),
                m.getContinut(), m.getTrimisLa());
    }
}

package backend.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/conversatii")
@Tag(name = "Conversatii", description = "Conversatii private si de grup; accesibile doar membrilor")
public class ChatController {

    public record UtilizatorDto(Long id, String nume, String email) { }
    public record ConversatieDto(Long id, String nume, List<UtilizatorDto> membri, LocalDateTime creatLa) { }
    public record MesajDto(Long id, UtilizatorDto autor, String continut, LocalDateTime trimisLa) { }

    public record ConversatieNoua(@Size(max = 100) String nume,
                                  @NotEmpty @Size(max = 50) List<@NotBlank @Email String> membri) { }
    public record MesajNou(@NotBlank @Size(max = 2000) String continut) { }

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    @Operation(summary = "Conversatiile mele", description = "Cele mai noi primele")
    public List<ConversatieDto> conversatii(@AuthenticationPrincipal Jwt jwt) {
        return chat.conversatiileMele(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creeaza o conversatie",
            description = "Fara nume: privata, cu exact un alt membru. Cu nume: grup. Creatorul e adaugat automat.")
    public ConversatieDto creeaza(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConversatieNoua req) {
        return chat.creeaza(jwt.getSubject(), req.nume(), req.membri());
    }

    @GetMapping("/{id}/mesaje")
    @Operation(summary = "Mesajele unei conversatii", description = "In ordine cronologica; 404 daca nu sunteti membru")
    public List<MesajDto> mesaje(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return chat.mesaje(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/mesaje")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Trimite un mesaj", description = "404 daca nu sunteti membru")
    public MesajDto trimite(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody MesajNou req) {
        return chat.trimite(jwt.getSubject(), id, req.continut());
    }
}

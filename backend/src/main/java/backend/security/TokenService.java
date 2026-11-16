package backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration valabilitate;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.valabilitate:PT1H}") Duration valabilitate) {
        this.encoder = encoder;
        this.valabilitate = valabilitate;
    }

    public String genereaza(Authentication auth) {
        Instant acum = Instant.now();
        List<String> roluri = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)     // ROLE_ADMIN
                .filter(a -> a.startsWith("ROLE_"))      // Spring Security 7 adauga si FACTOR_PASSWORD, care nu e rol
                .map(a -> a.replaceFirst("^ROLE_", "")) // ADMIN
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chatapp")
                .subject(auth.getName()) // emailul
                .issuedAt(acum)
                .expiresAt(acum.plus(valabilitate))
                .claim("roles", roluri)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long valabilitateSecunde() {
        return valabilitate.toSeconds();
    }
}

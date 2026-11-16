package backend.security;

import backend.repository.UtilizatorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Spune lui Spring Security cum gaseste un utilizator dupa email in tabela noastra. */
@Service
public class UtilizatorDetailsService implements UserDetailsService {

    private final UtilizatorRepository utilizatori;

    public UtilizatorDetailsService(UtilizatorRepository utilizatori) {
        this.utilizatori = utilizatori;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        // Spring transforma exceptia in "parola gresita": nu aflam din raspuns ce emailuri exista
        return utilizatori.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getParolaHash())
                        .roles(u.getRol().name()) // ADMIN -> ROLE_ADMIN
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Utilizator inexistent"));
    }
}

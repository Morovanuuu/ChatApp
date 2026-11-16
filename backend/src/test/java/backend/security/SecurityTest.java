package backend.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxuri reale: utilizatorii din V2, tokenuri adevarate. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    MockMvc mvc;

    private String token(String email, String parola) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"parola\":\"%s\"}".formatted(email, parola)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void loginCorectDaTokenFolositLaEu() throws Exception {
        String token = token("admin@chatapp.ro", "admin1234");

        mvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@chatapp.ro"))
                .andExpect(jsonPath("$.roluri[0]").value("ADMIN"));
    }

    @Test
    void parolaGresitaDa401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@chatapp.ro\",\"parola\":\"gresita\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Autentificare esuata"));
    }

    @Test
    void emailInexistentDaAcelasi401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimeni@chatapp.ro\",\"parola\":\"orice123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autentificare esuata"));
    }

    @Test
    void faraTokenDa401ProblemDetail() throws Exception {
        mvc.perform(get("/api/auth/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Neautentificat"));
    }

    @Test
    void tokenModificatEsteRespins() throws Exception {
        // Ca in Lab 4, exp. 8.3: schimbam USER in ADMIN in payload si pastram semnatura veche
        String[] parti = token("ana@chatapp.ro", "ana12345").split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parti[1]), StandardCharsets.UTF_8)
                .replace("\"USER\"", "\"ADMIN\"");
        String falsificat = parti[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parti[2];

        mvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + falsificat))
                .andExpect(status().isUnauthorized());
    }
}

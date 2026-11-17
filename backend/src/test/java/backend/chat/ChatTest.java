package backend.chat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxuri reale prin HTTP (MockMvc) cu utilizatorii din V2; rollback dupa fiecare test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatTest {

    @Autowired
    MockMvc mvc;

    private String token(String email, String parola) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"parola\":\"%s\"}".formatted(email, parola)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private ResultActions posteaza(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int conversatiePrivataAnaMihai(String ana) throws Exception {
        String body = posteaza(ana, "/api/conversatii", "{\"membri\":[\"mihai@chatapp.ro\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nume").doesNotExist())
                .andExpect(jsonPath("$.membri.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void anaScrieMihaiCiteste() throws Exception {
        String ana = token("ana@chatapp.ro", "ana12345");
        String mihai = token("mihai@chatapp.ro", "mihai1234");
        int id = conversatiePrivataAnaMihai(ana);

        posteaza(ana, "/api/conversatii/" + id + "/mesaje", "{\"continut\":\"Salut, Mihai!\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor.email").value("ana@chatapp.ro"))
                .andExpect(jsonPath("$.trimisLa").exists());
        posteaza(mihai, "/api/conversatii/" + id + "/mesaje", "{\"continut\":\"Salut, Ana!\"}")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/conversatii/" + id + "/mesaje").header("Authorization", mihai))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].continut").value("Salut, Mihai!"))
                .andExpect(jsonPath("$[1].autor.nume").value("Mihai Ionescu"));

        mvc.perform(get("/api/conversatii").header("Authorization", mihai))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].membri.length()").value(2));
    }

    @Test
    void cineNuEsteMembruPrimeste404() throws Exception {
        int id = conversatiePrivataAnaMihai(token("ana@chatapp.ro", "ana12345"));
        String admin = token("admin@chatapp.ro", "admin1234");

        mvc.perform(get("/api/conversatii/" + id + "/mesaje").header("Authorization", admin))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        posteaza(admin, "/api/conversatii/" + id + "/mesaje", "{\"continut\":\"intrus\"}")
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/conversatii").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void dateInvalideDau400() throws Exception {
        String ana = token("ana@chatapp.ro", "ana12345");
        int id = conversatiePrivataAnaMihai(ana);

        posteaza(ana, "/api/conversatii/" + id + "/mesaje", "{\"continut\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        posteaza(ana, "/api/conversatii", "{\"membri\":[\"nimeni@chatapp.ro\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Utilizator inexistent: nimeni@chatapp.ro"));
        // privata cu doi ceilalti membri: trebuie nume de grup
        posteaza(ana, "/api/conversatii", "{\"membri\":[\"mihai@chatapp.ro\",\"admin@chatapp.ro\"]}")
                .andExpect(status().isBadRequest());
        // doar pe sine
        posteaza(ana, "/api/conversatii", "{\"membri\":[\"ana@chatapp.ro\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void grupCuTreiMembri() throws Exception {
        posteaza(token("ana@chatapp.ro", "ana12345"), "/api/conversatii",
                "{\"nume\":\"Proiect\",\"membri\":[\"mihai@chatapp.ro\",\"admin@chatapp.ro\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nume").value("Proiect"))
                .andExpect(jsonPath("$.membri.length()").value(3));
    }

    @Test
    void faraTokenDa401() throws Exception {
        mvc.perform(get("/api/conversatii")).andExpect(status().isUnauthorized());
    }
}

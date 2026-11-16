package backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Erorile de securitate apar in filtre, inainte de controller, deci @ExceptionHandler nu le vede.
 * Le scriem aici in acelasi format ProblemDetail ca restul API-ului.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override // 401: lipseste tokenul sau este invalid / expirat
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        res.setHeader("WWW-Authenticate", "Bearer");
        scrie(req, res, HttpStatus.UNAUTHORIZED, "Neautentificat",
                "Trimiteti un token valid in header-ul Authorization: Bearer <token>");
    }

    @Override // 403: tokenul este valid, dar rolul nu ajunge
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {
        scrie(req, res, HttpStatus.FORBIDDEN, "Acces interzis",
                "Contul dumneavoastra nu are dreptul sa faca aceasta operatie");
    }

    private void scrie(HttpServletRequest req, HttpServletResponse res, HttpStatus status,
                       String titlu, String detaliu) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detaliu);
        pd.setTitle(titlu);
        pd.setInstance(URI.create(req.getRequestURI()));
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), pd);
    }
}

package cl.vidasalud.bff.controller;

import cl.vidasalud.bff.client.Downstream;
import cl.vidasalud.bff.client.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * Enruta /api/appointments/**, /api/catalog/** y /api/admin/** hacia su microservicio.
 * La autorización por rol ya fue aplicada por SecurityConfig antes de llegar aquí;
 * los microservicios reciben la identidad en X-User-Email / X-User-Roles.
 */
@RestController
public class ProxyController {
    private final Downstream appointments;
    private final Downstream catalog;
    private final Downstream rabbitAdmin;

    public ProxyController(@Qualifier("appointments") Downstream appointments,
                           @Qualifier("catalog") Downstream catalog,
                           @Qualifier("rabbitAdmin") Downstream rabbitAdmin) {
        this.appointments = appointments;
        this.catalog = catalog;
        this.rabbitAdmin = rabbitAdmin;
    }

    @RequestMapping(path = {"/api/appointments", "/api/appointments/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> appointments(HttpServletRequest req, @RequestBody(required = false) String body,
                                               Authentication auth) {
        return forward(appointments, req, body, auth);
    }

    @RequestMapping(path = {"/api/catalog/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> catalog(HttpServletRequest req, @RequestBody(required = false) String body,
                                          Authentication auth) {
        return forward(catalog, req, body, auth);
    }

    @RequestMapping(path = {"/api/admin/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
    public ResponseEntity<String> admin(HttpServletRequest req, @RequestBody(required = false) String body,
                                        Authentication auth) {
        return forward(rabbitAdmin, req, body, auth);
    }

    private static ResponseEntity<String> forward(Downstream target, HttpServletRequest req, String body,
                                                  Authentication auth) {
        JwtAuthenticationToken t = (JwtAuthenticationToken) auth;
        String path = req.getRequestURI() + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
        ResponseEntity<String> r = target.call(HttpMethod.valueOf(req.getMethod()), path, body,
                UserContext.from(t.getToken(), t.getAuthorities()));
        ResponseEntity.BodyBuilder out = ResponseEntity.status(r.getStatusCode());
        return r.getBody() == null ? out.build() : out.contentType(MediaType.APPLICATION_JSON).body(r.getBody());
    }
}

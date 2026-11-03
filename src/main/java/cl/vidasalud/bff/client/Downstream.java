package cl.vidasalud.bff.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Proxy HTTP hacia un microservicio interno. Reenvía el cuerpo tal cual y devuelve
 * el status/cuerpo original (incluidos 400/422), propagando la identidad del usuario.
 */
public class Downstream {
    private final RestClient client;

    public Downstream(RestClient.Builder builder, String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public ResponseEntity<String> call(HttpMethod method, String pathAndQuery, String body, UserContext user) {
        RestClient.RequestBodySpec spec = client.method(method).uri(pathAndQuery)
                .header("X-User-Email", user.email())
                .header("X-User-Roles", user.roles());
        if (body != null && !body.isBlank()) {
            spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return spec.retrieve()
                .onStatus(status -> true, (req, res) -> { /* no lanzar: se devuelve tal cual */ })
                .toEntity(String.class);
    }
}

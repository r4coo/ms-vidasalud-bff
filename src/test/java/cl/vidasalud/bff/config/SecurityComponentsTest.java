package cl.vidasalud.bff.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityComponentsTest {
    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("tok", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "none"), claims);
    }

    @Test
    void audience_aceptaApiPropia() {
        var v = new AudienceValidator(List.of("api-test", "api://api-test"));
        assertFalse(v.validate(jwt(Map.of("aud", List.of("api://api-test"), "sub", "u"))).hasErrors());
    }

    @Test
    void audience_rechazaTokenParaOtraApi() {
        var v = new AudienceValidator(List.of("api-test", "api://api-test"));
        assertTrue(v.validate(jwt(Map.of("aud", List.of("otra-api"), "sub", "u"))).hasErrors());
    }

    @Test
    void issuer_aceptaTenantPropioYRechazaOtros() {
        var v = new IssuerValidator("tenant-1");
        assertFalse(v.validate(jwt(Map.of("iss", "https://login.microsoftonline.com/tenant-1/v2.0", "sub", "u"))).hasErrors());
        assertTrue(v.validate(jwt(Map.of("iss", "https://login.microsoftonline.com/otro/v2.0", "sub", "u"))).hasErrors());
    }

    @Test
    void roles_seMapeanAAuthorities() {
        var auth = new EntraRolesConverter().convert(jwt(Map.of("roles", List.of("Admin", "Recepcionista"), "sub", "u")));
        List<String> names = auth.getAuthorities().stream().map(Object::toString).toList();
        assertEquals(List.of("ROLE_Admin", "ROLE_Recepcionista"), names);
    }

    @Test
    void sinRoles_sinAuthorities() {
        var auth = new EntraRolesConverter().convert(jwt(Map.of("sub", "u")));
        assertTrue(auth.getAuthorities().isEmpty());
    }
}

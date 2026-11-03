package cl.vidasalud.bff.client;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identidad extraída del JWT ya validado. */
public record UserContext(String email, String roles) {
    public static UserContext from(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        String email = firstNonBlank(jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"), jwt.getClaimAsString("upn"), jwt.getSubject());
        String roles = authorities.stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).collect(Collectors.joining(","));
        return new UserContext(email, roles);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return "desconocido";
    }
}

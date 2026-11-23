package cl.vidasalud.bff.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * El BFF valida de nuevo el JWT de Entra ID (firma, issuer, audience y vigencia) y aplica
 * la autorización por rol. Aunque API Gateway ya lo valide, el BFF no confía en nadie.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${app.security.cors.allowed-origins}") private String allowedOrigins;
    @Value("${app.security.entra.tenant-id}") private String tenantId;
    @Value("${app.security.entra.api-client-id}") private String apiClientId;
    @Value("${app.security.entra.jwk-set-uri}") private String jwkSetUri;

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // createDefault() valida exp / nbf (vigencia); además issuer y audience.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new IssuerValidator(tenantId),
                new AudienceValidator(List.of(apiClientId, "api://" + apiClientId))));
        return decoder;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/api/health").permitAll()
                // Administración de RabbitMQ y de Kafka: solo Admin
                .requestMatchers("/api/admin/**").hasRole("Admin")
                // Métricas en tiempo real (Kafka): Admin y Recepcionista, solo lectura
                .requestMatchers(HttpMethod.GET, "/api/analytics/**").hasAnyRole("Admin", "Recepcionista")
                // Catálogo: lectura para todos los roles; crear/editar Admin y Recepcionista; borrar solo Admin
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("Admin", "Recepcionista", "Paciente")
                // Borrar del catálogo: solo Admin
                .requestMatchers(HttpMethod.DELETE, "/api/catalog/**").hasRole("Admin")
                .requestMatchers("/api/catalog/**").hasAnyRole("Admin", "Recepcionista")
                // Atenciones: los tres roles (el microservicio refina: el paciente solo ve/cancela las suyas)
                .requestMatchers("/api/appointments/**").hasAnyRole("Admin", "Recepcionista", "Paciente")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(new EntraRolesConverter())));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}

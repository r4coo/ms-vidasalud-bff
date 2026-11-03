package cl.vidasalud.bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Acepta tokens v2.0 (login.microsoftonline.com/{tenant}/v2.0) y v1.0 (sts.windows.net/{tenant}/) del tenant. */
public class IssuerValidator implements OAuth2TokenValidator<Jwt> {
    private final String tenantId;

    public IssuerValidator(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String iss = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
        if (iss.equals("https://login.microsoftonline.com/" + tenantId + "/v2.0")
                || iss.equals("https://sts.windows.net/" + tenantId + "/")) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "issuer inválido", null));
    }
}

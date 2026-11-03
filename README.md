# ms-vidasalud-bff

BFF de VidaSalud. Valida el JWT de Microsoft Entra ID (firma por JWKS, issuer v1/v2, audience, exp/nbf), convierte los App roles
(`Admin`, `Recepcionista`, `Paciente`) en autoridades y enruta a los microservicios propagando `X-User-Email` / `X-User-Roles`.
No tiene base de datos.

- Puerto: **8084**
- Variables: `ENTRA_TENANT_ID`, `ENTRA_API_CLIENT_ID`, `FRONTEND_URLS`, `ENDPOINT_*` (ver `application.properties`).
- Respuestas: 401 sin token/token inválido, 403 sin rol suficiente.

```bash
./gradlew test
./gradlew bootRun
```

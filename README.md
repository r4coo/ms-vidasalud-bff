# ms-vidasalud-bff

BFF de VidaSalud. Valida el JWT de Microsoft Entra ID (firma por JWKS, issuer v1/v2, audience, exp/nbf), convierte los App roles
(`Admin`, `Recepcionista`, `Paciente`) en autoridades y enruta a los microservicios propagando `X-User-Email` / `X-User-Roles`.
No tiene base de datos.

Rutas a los servicios: appointments (8082), catalog (8087), notification (8085, analítica), rabbit-admin (8088) y kafka-admin (8089).

- Puerto: **8084**
- Variables: `ENTRA_TENANT_ID`, `ENTRA_API_CLIENT_ID`, `FRONTEND_URLS`, `ENDPOINT_*` (ver `application.properties`).
- Respuestas: 401 sin token/token inválido, 403 sin rol suficiente. Mientras un servicio interno aún está arrancando, el BFF puede responder 403 en lugar de 502/503 (esperar ~90 s tras reconstruir).
- CORS: orígenes de `FRONTEND_URLS` (EC2/Caddy, Vercel y `localhost:4200`).

| Ruta | Roles permitidos |
| --- | --- |
| `/api/admin/rabbit/**` | Admin (RabbitMQ: colas, exchanges, bindings) |
| `/api/admin/kafka/**` | Admin (Kafka: tópicos, parámetros, grupos, lag) |
| `GET /api/analytics/**` | Admin, Recepcionista (métricas en tiempo real desde Kafka) |
| `GET /api/catalog/**` | Admin, Recepcionista, Paciente |
| `DELETE /api/catalog/**` | Admin |
| `POST/PUT /api/catalog/**` | Admin, Recepcionista |
| `/api/appointments/**` | Admin, Recepcionista, Paciente (el servicio refina por dueño) |
| cualquier otra | denegada |

```bash
./gradlew test
./gradlew bootRun
```

# ms-auth — Microservicio de Identidad (Pedidos360)

BFF de identidad. **El login real lo hace Azure AD** (Authorization Code + PKCE);
este servicio valida el JWT recibido y expone el perfil del usuario, además de
**auditar cada acceso** en su propia base de datos (`auth_db`).

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/auth/me` | Autenticado. Devuelve `{ id, nombre, email, roles, scopes }` y registra el acceso. |
| GET | `/api/auth/accesos?limite=50` | Autenticado. El usuario ve los suyos; un `ADMIN` ve todos. |
| GET | `/api/auth/health` | Público. |
| GET | `/actuator/health` | Público. |

Valida el JWT igual que los otros microservicios: firma (JWKS de Azure), vigencia,
`issuer` y `audience`. Roles y scopes salen de los claims `roles` / `scp`
(con el fallback por email si el tenant no asigna App Roles).

## Configuración (variables de entorno)

| Variable | Descripción | Defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC de MySQL | `jdbc:mysql://localhost:3306/auth_db` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Credenciales BD | `pedidos` / `pedidos` |
| `AZURE_JWK_SET_URI` / `AZURE_ISSUER` / `AZURE_AUDIENCE` | IDaaS | ver `docs/AZURE-AD-SETUP.md` |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origen del frontend | `http://localhost:3000` |
| `APP_ROLES_ADMIN` / `APP_ROLES_VENDEDOR` | Emails → rol (fallback) | vacío |

## Ejecutar

```bash
mvn clean verify
```
```bash
mvn spring-boot:run
```

Flyway crea la tabla `accesos` al arrancar.

## Pruebas

- `AuthControllerTest` — `/health` público, `/me` sin token → 401, `/me` con token →
  perfil correcto + acceso auditado (visible en `/accesos`).

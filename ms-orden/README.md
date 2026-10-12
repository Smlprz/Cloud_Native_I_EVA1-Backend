# ms-orden

Microservicio de ordenes (puerto 8085 en local, 8080 dentro del contenedor).
Estructura base copiada de `ms-auth`: seguridad Azure AD (JWT), JPA + Flyway + MySQL (`orden_db`), Dockerfile.

## Endpoints
| Metodo | Ruta | Scope | Roles |
|---|---|---|---|
| GET | `/api/ordenes` | `ordenes.read` | CLIENTE, ADMIN |
| POST | `/api/ordenes` | `ordenes.write` | CLIENTE, ADMIN |

Body del POST: `{"total": 15990}`.

## Pendiente (AWS / Azure)
- Crear los scopes `ordenes.read` y `ordenes.write` en el App Registration de la API.
- Crear el schema `orden_db` en MySQL y su EC2 con `infra/deploy-ec2`.
- Agregar las rutas al `infra/api-gateway/openapi.yaml`.

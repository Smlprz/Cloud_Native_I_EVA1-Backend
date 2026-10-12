# ms-resenas

Microservicio de reseñas de productos (puerto 8086 en la EC2, 8080 dentro del contenedor).
Misma estructura que `ms-orden`: seguridad Azure AD (JWT), JPA + Flyway + MySQL (`resenas_db`), Dockerfile.

## Endpoints
| Metodo | Ruta | Scope | Roles |
|---|---|---|---|
| GET | `/api/resenas?productoId=1` | `resenas.read` | cualquier usuario autenticado |
| POST | `/api/resenas` | `resenas.write` | CLIENTE, ADMIN |
| DELETE | `/api/resenas/{id}` | `resenas.write` | CLIENTE, ADMIN (solo el autor) |

Body del POST: `{"productoId": 1, "puntaje": 5, "comentario": "Excelente"}` (puntaje de 1 a 5).

## Pendiente (AWS / Azure)
- Crear los scopes `resenas.read` y `resenas.write` en el App Registration de la API.
- Crear el schema `resenas_db` en MySQL (ver `infra/mysql/init/01-init-databases.sql`).
- Levantar la EC2 con `infra/deploy-ec2/ec2-resenas/docker-compose.yml`.
- Agregar las rutas al `infra/api-gateway/openapi.yaml`.

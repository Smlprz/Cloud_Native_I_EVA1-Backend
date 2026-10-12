# ms-orden

Microservicio de ordenes (puerto 8085 en local, 8080 dentro del contenedor).
Estructura base copiada de `ms-auth`: seguridad Azure AD (JWT), JPA + Flyway + MySQL (`orden_db`), Dockerfile.

## Endpoints
| Metodo | Ruta | Scope | Roles |
|---|---|---|---|
| GET | `/api/ordenes` | `ordenes.read` | CLIENTE, ADMIN |
| POST | `/api/ordenes` | `ordenes.write` | CLIENTE, ADMIN |

Body del POST: `{"total": 15990}`.

## Flujo con RabbitMQ
`POST /api/ordenes` guarda la orden y publica `{ordenId, email, total}` en el exchange `pedidos.exchange`
(routing key `orden.creada`). `ms-notificaciones` consume la cola `orden.creada` y envia el correo;
si falla 3 veces el mensaje pasa a `orden.creada.dlq` y se registra en el log.
`ms-carrito` llama a este endpoint tras el checkout reenviando el token del usuario
(por eso tambien se aceptan los scopes `carrito.read` / `carrito.write`).

## Variables de entorno
`SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` (ver `infra/deploy-ec2/ec2-orden`).

## Pendiente (AWS)
- Crear el schema `orden_db` en MySQL y la EC2 con `infra/deploy-ec2/ec2-orden`.
- Levantar RabbitMQ con `infra/deploy-ec2/ec2-rabbitmq` y pasar su IP en `RABBITMQ_EC2_HOST`.
- Agregar las rutas al `infra/api-gateway/openapi.yaml`.
- Opcional: crear los scopes `ordenes.read` / `ordenes.write` en `pedidos360-api`.

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
`POST /api/ordenes` guarda la orden y publica dos mensajes en el exchange **direct** `pedidos.exchange`:

| Routing key | Contenido | Lo consume |
|---|---|---|
| `orden.creada` | `{ordenId, email, total}` | `ms-notificaciones` (correo de confirmación) |
| `stock.descontar` | `{ordenId, items:[{productoId, cantidad}]}` — solo si el POST trae `items` | `ms-productos` (descuenta el stock) |

`ms-carrito` llama a este endpoint tras el checkout reenviando el token del usuario y los ítems
comprados (por eso también se aceptan los scopes `carrito.read` / `carrito.write`).

Body del POST: `{"total": 15990, "email": "opcional@correo.cl", "items": [{"productoId": 1, "cantidad": 2}]}`
(`email` e `items` son opcionales; `items` se valida: `productoId` obligatorio y `cantidad` > 0).

Si RabbitMQ falla, la orden ya guardada no se pierde: se registra el error en el log.
Los nombres de exchange y routing keys viven en `application.yml` (bloque `messaging:`) y se
leen con `MessagingProperties`; `RabbitTopologyConfig` declara el exchange y `RabbitClientConfig`
configura el `RabbitTemplate` (confirmaciones del broker y mensajes no enrutados al log).

## Variables de entorno
`SPRING_RABBITMQ_ADDRESSES` (`host:5672,host:5673`, o `SPRING_RABBITMQ_HOST`/`PORT` para un solo nodo),
`SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` (ver `infra/deploy-ec2/ec2-orden`).

## Pendiente (AWS)
- Crear el schema `orden_db` en MySQL y la EC2 con `infra/deploy-ec2/ec2-orden`.
- Levantar el clúster RabbitMQ con `infra/deploy-ec2/ec2-rabbitmq` y pasar su IP en `RABBITMQ_EC2_HOST`.
- Agregar las rutas al `infra/api-gateway/openapi.yaml`.
- Opcional: crear los scopes `ordenes.read` / `ordenes.write` en la API de Azure.

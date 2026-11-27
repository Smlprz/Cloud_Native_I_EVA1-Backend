# ms-resenas

**Microservicio nuevo de Pedidos360**: los clientes califican (1 a 5) y comentan los productos, y cada reseña
alimenta la calificación promedio del catálogo **a través de RabbitMQ**.

Puerto 8086 en la EC2 (8080 dentro del contenedor). Seguridad Azure AD (JWT), JPA + Flyway + MySQL (`resenas_db`), Dockerfile.
Forma parte del backend de Pedidos360: https://github.com/Smlprz/Cloud_Native_I_EVA1-Backend (rama `evaluacion-3-kafka`).

## Integración con RabbitMQ
```
POST /api/resenas --> ms-resenas guarda la reseña
                  --resena.creada--> [pedidos.exchange : direct] --> cola resena.creada --> ms-productos
                                                                    (rechazo) --> pedidos.dlx --> resena.creada.dlq
ms-productos suma el puntaje al promedio del producto (GET /api/productos/{id}: calificacionPromedio, resenasCantidad)
```
- Mensaje: `{"resenaId": 12, "productoId": 1, "puntaje": 5}` (`ResenaCreadaEvent`).
- Productor: `messaging/ResenaPublisher` (publica en el exchange direct; si RabbitMQ falla, la reseña ya guardada no se pierde y queda en el log).
- Consumidor: `ms-productos` (`ResenaCreadaConsumer`), con ACK manual, idempotente (`resenas_procesadas`) y DLQ.
- Nombres de exchange, cola, DLQ y routing key en `application.yml` (bloque `messaging:`).

## Endpoints
| Metodo | Ruta | Scope | Roles |
|---|---|---|---|
| GET | `/api/resenas?productoId=1` | `resenas.read` | cualquier usuario autenticado |
| POST | `/api/resenas` | `resenas.write` | CLIENTE, ADMIN |
| DELETE | `/api/resenas/{id}` | `resenas.write` | CLIENTE, ADMIN (solo el autor) |

Body del POST: `{"productoId": 1, "puntaje": 5, "comentario": "Excelente"}` (puntaje de 1 a 5).

## Ejecutar
- Local: `mvn spring-boot:run` (necesita MySQL y RabbitMQ) o el `docker-compose.yml` de `infra/` del backend.
- AWS: `infra/deploy-ec2/ec2-resenas/docker-compose.yml` con `PRODUCTOS_EC2_HOST` (MySQL) y `RABBITMQ_EC2_HOST`.
- Pruebas: `mvn test`.

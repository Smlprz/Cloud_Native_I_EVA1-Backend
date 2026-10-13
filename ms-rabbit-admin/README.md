# ms-rabbit-admin

Microservicio administrador de RabbitMQ (puerto 8087 en la EC2, 8080 dentro del contenedor).
Crea y elimina colas, exchanges y bindings usando `AmqpAdmin` de Spring AMQP. No usa base de datos.
Seguridad: JWT de Azure AD (app `pedidos360-api`), rol **ADMIN** y scope `rabbit.admin`
(o `carrito.write` mientras `rabbit.admin` no exista en Azure).

## Endpoints (`/api/rabbit`)
| Metodo | Ruta | Body |
|---|---|---|
| POST | `/exchanges` | `{"name":"ex","type":"direct\|topic\|fanout\|headers","durable":true}` |
| DELETE | `/exchanges/{name}` | |
| POST | `/queues` | `{"name":"q","durable":true,"deadLetterExchange":"dlx","deadLetterRoutingKey":"q.dlq"}` |
| GET | `/queues/{name}` | devuelve mensajes y consumidores |
| DELETE | `/queues/{name}` | |
| POST | `/bindings` | `{"queue":"q","exchange":"ex","routingKey":"rk"}` |
| DELETE | `/bindings` | mismo body que el POST |

Cuidado: borrar `orden.creada` o `pedidos.exchange` rompe el flujo de ordenes hasta que `ms-notificaciones` reinicie (los redeclara).

## Variables de entorno
`SPRING_RABBITMQ_HOST/USERNAME/PASSWORD`, `APP_ROLES_ADMIN` (correos con rol ADMIN), `AZURE_*`.

## Pendiente (AWS / Azure)
- Opcional: crear el scope `rabbit.admin` en `pedidos360-api`.
- EC2 con `infra/deploy-ec2/ec2-rabbit-admin` y puerto 8087 abierto.
- Rutas `/rabbit/**` en el gateway.

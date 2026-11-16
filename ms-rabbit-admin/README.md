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

Cuidado: borrar `orden.creada`, `stock.descontar`, `stock.bajo`, `pedidos.exchange` o `pedidos.eventos` rompe el flujo
de ordenes hasta que el servicio dueño de la cola reinicie (los redeclara).

## Validacion y errores
- Los nombres (cola, exchange, binding) deben tener 1-255 caracteres: letras, numeros y `. _ : -`, empezar con
  letra o numero y **no** usar el prefijo reservado `amq.`. La routing key admite tambien `*` y `#`.
- `deadLetterRoutingKey` exige `deadLetterExchange`.
- Respuestas de error (RFC 7807): **400** con el detalle campo a campo, **404** cola inexistente,
  **409** el broker rechazo la operacion (por ejemplo redeclarar una cola con otros argumentos),
  **503** sin conexion con RabbitMQ.
- La logica de RabbitMQ esta encapsulada en `RabbitAdminService`; el controlador solo valida y delega.

## Variables de entorno
`SPRING_RABBITMQ_ADDRESSES` (`host:5672,host:5673`; o `SPRING_RABBITMQ_HOST`/`PORT`), `SPRING_RABBITMQ_USERNAME/PASSWORD`, `APP_ROLES_ADMIN` (correos con rol ADMIN), `AZURE_*`.

## Pendiente (AWS / Azure)
- Opcional: crear el scope `rabbit.admin` en `pedidos360-api`.
- EC2 con `infra/deploy-ec2/ec2-rabbit-admin` y puerto 8087 abierto.
- Rutas `/rabbit/**` en el gateway.

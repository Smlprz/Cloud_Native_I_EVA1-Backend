# ms-notificaciones

Microservicio de correo (puerto 8084 en la EC2, 8080 dentro del contenedor). **Spring Boot 4**. Sin base de datos.
Seguridad: JWT de Azure AD (el endpoint de prueba exige rol ADMIN).

## Mensajería (RabbitMQ)
| Cola | Exchange / routing key | Qué hace |
|---|---|---|
| `orden.creada` | direct `pedidos.exchange` / `orden.creada` | Envía el correo de confirmación de la compra |
| `stock.bajo` | topic `pedidos.eventos` / `stock.bajo` | Avisa por correo (a `STOCK_ALERT_EMAIL`) que un producto se agota |

Cada cola tiene su DLQ (`orden.creada.dlq`, `stock.bajo.dlq`) enlazada al DLX `pedidos.dlx`.

- **ACK manual** (`messaging.support.AckSupport`): éxito → ACK; mensaje inválido (JSON roto, sin email) → NACK a
  la DLQ sin reintentos; fallo del SMTP → hasta 3 reintentos (`messaging.retry.*`) y luego DLQ.
  Cada mensaje enviado a la DLQ queda en el log con su causa y su cuerpo. La DLQ no se consume: los mensajes
  muertos quedan visibles en el panel de RabbitMQ.
- Los nombres de colas y exchanges viven en `application.yaml` (bloque `messaging:`).
- Estructura: `messaging.config` (propiedades + `RabbitTopologyConfig`), `messaging.orden` y `messaging.stock`
  (un consumidor por dominio), `messaging.support`.

## Endpoint de prueba
`POST /api/email/enviar?to=&subject=&body=` (rol ADMIN) — envía un correo directamente, por ejemplo desde Postman.

## Variables de entorno
| Variable | Descripción |
|---|---|
| `SPRING_RABBITMQ_ADDRESSES` | Nodos del clúster `host:5672,host:5673` (o `SPRING_RABBITMQ_HOST`/`PORT`) |
| `SPRING_RABBITMQ_USERNAME` / `PASSWORD` | Credenciales de RabbitMQ |
| `GMAIL_USERNAME` / `GMAIL_APP_PASSWORD` | Cuenta Gmail y contraseña de aplicación (**nunca** en el repositorio) |
| `MAIL_SIMULADO` | `true` = el correo solo se escribe en el log (demo sin Gmail) |
| `STOCK_ALERT_EMAIL` | Destino de la alerta de stock bajo (por defecto `GMAIL_USERNAME`; vacío = solo log) |
| `AZURE_*`, `APP_ROLES_ADMIN` | IDaaS y roles |

# Pedidos360 — Backend (Cloud Native I)

Backend de microservicios de la tienda **Pedidos360**. Identidad delegada a
**Azure AD**, tráfico perimetral por **AWS API Gateway**, microservicios Spring Boot
en contenedores sobre **EC2** con **MySQL**, mensajería asíncrona con **RabbitMQ** (clúster de 2 nodos) y
analítica en tiempo real con **Apache Kafka** (3 Zookeeper + 3 brokers).

## Microservicios

| Servicio | Puerto | BD | Rol |
|---|---|---|---|
| [`ms-productos`](ms-productos/) | 8081 | `productos_db` | Catálogo. GET público; escrituras con rol. **Consume `stock.descontar`**, publica `stock.bajo` y **publica `PRODUCTO_VISTO` en Kafka**. |
| [`ms-carrito`](ms-carrito/) | 8082 | `carrito_db` | Carrito y pedidos. Al confirmar el checkout llama a `ms-orden` (con los ítems). **Publica eventos de carrito y compra en Kafka**. |
| [`ms-auth`](ms-auth/) | 8083 | `auth_db` | Identidad (BFF): valida el JWT de Azure, expone el perfil y **audita los accesos**. |
| [`ms-notificaciones`](ms-notificaciones/) | 8084 | — | Correo (Gmail). **Consume `orden.creada` y `stock.bajo`**. |
| [`ms-orden`](ms-orden/) | 8085 | `orden_db` | Órdenes. **Publica `orden.creada` y `stock.descontar`**. |
| [`ms-resenas`](ms-resenas/) | 8086 | `resenas_db` | Reseñas de productos. |
| [`ms-rabbit-admin`](ms-rabbit-admin/) | 8087 | — | Administra RabbitMQ por REST: crea/elimina colas, exchanges y bindings. |
| [`ms-pago`](ms-pago/) | 8088 | `pago_db` | Pago simulado de una orden con **antifraude** (misma tarjeta > 3 veces en 10 s se bloquea). **Publica en Kafka `eventos.pagos`**. |
| [`ms-analytics-recommendations`](ms-analytics-recommendations/) | 8089 | DynamoDB | **Consume Kafka**: perfiles de navegación, **recomendaciones** y **métricas en tiempo real por WebSocket**. |
| [`ms-cupones`](ms-cupones/) | 8092 | `cupones_db` | **Microservicio libre**: cupones de descuento. `ms-carrito` los aplica en el checkout (el total baja) y publica `CUPON_APLICADO` en Kafka. |
| [`ms-kafka-admin`](ms-kafka-admin/) | 8091 | — | Administra Kafka por REST: crea, modifica y elimina tópicos y consulta consumer groups, offsets y lag. |

Todos validan el JWT de Azure (firma vía JWKS + vigencia + `issuer` + `audience`).
Roles: del claim `roles` si existe (normalizado `Administrador`→ADMIN…), o por email
(`APP_ROLES_ADMIN` / `APP_ROLES_VENDEDOR`), con `CLIENTE` por defecto.

## Mensajería (RabbitMQ)

```
ms-carrito --HTTP--> ms-orden --orden.creada----> [pedidos.exchange : direct] --> orden.creada    --> ms-notificaciones (correo)
                              --stock.descontar-> [pedidos.exchange : direct] --> stock.descontar --> ms-productos (descuenta stock)
                                  ms-productos --stock.bajo--> [pedidos.eventos : topic] --> stock.bajo --> ms-notificaciones (alerta)
Mensaje rechazado en cualquier cola --> [pedidos.dlx : direct] --> orden.creada.dlq / stock.descontar.dlq / stock.bajo.dlq
```

- **3 colas con su DLQ**, exchanges **direct** y **topic**, y un DLX direct para los mensajes muertos.
- Los nombres se definen **una sola vez por servicio** en `application.yml` (bloque `messaging:`);
  cada servicio declara sus beans `Queue`/`Exchange`/`Binding` en `RabbitTopologyConfig`.
- Los consumidores usan **ACK manual** (`AckSupport`): éxito → ACK; mensaje inválido o error de
  negocio → NACK a la DLQ sin reintentos; error técnico → hasta 3 reintentos y luego DLQ. Cada mensaje
  enviado a una DLQ queda en el log con su causa y su cuerpo. Las DLQ **no** se consumen: los mensajes
  muertos quedan visibles en el panel de RabbitMQ.
- Consumidores **idempotentes** (el descuento de stock registra la orden en `stock_movimientos`).
- El clúster de 2 nodos está en [`infra/deploy-ec2/ec2-rabbitmq`](infra/deploy-ec2/ec2-rabbitmq/)
  (los servicios se conectan con `SPRING_RABBITMQ_ADDRESSES=host:5672,host:5673`).

## Streaming y analítica (Kafka)

```
ms-productos --PRODUCTO_VISTO-------------> [eventos.navegacion] --> grupo analytics-recomendaciones --> perfil + recomendaciones (DynamoDB)
ms-carrito   --ITEM_AGREGADO/ELIMINADO/                           --> grupo analytics-metricas        --> métricas --> WebSocket --> dashboard
               COMPRA_REALIZADA---------->  [eventos.navegacion]
ms-pago      --PAGO_APROBADO/RECHAZADO/
               FRAUDE_BLOQUEADO---------->  [eventos.pagos]       --> grupo analytics-metricas
```

- **Clúster** en [`infra/deploy-ec2/ec2-kafka`](infra/deploy-ec2/ec2-kafka/): 3 Zookeeper + 3 brokers (`docker-compose.yml`),
  **Kafka-UI** (`docker-compose.ui.yml`, puerto 8090) y `ms-kafka-admin` (`docker-compose.admin.yml`).
- **2 tópicos con 3 particiones y 3 réplicas** (`min.insync.replicas=2`: soportan la caída de un broker sin perder datos):
  `eventos.navegacion` (retención 7 días) y `eventos.pagos` (30 días), con `cleanup.policy=delete`. Se crean al levantar el clúster.
- **Productores**: `acks=all` + idempotencia, JSON tipado sin cabecera de tipo Java, `max.block.ms` corto (si Kafka cae, la API
  de productos o carrito sigue funcionando). La clave del mensaje es el usuario: sus eventos van a una sola partición, en orden.
- **Consumidores** (`@KafkaListener`, `concurrency=3`, offsets por registro): dos **consumer groups** leen el mismo tópico de forma independiente.
  Un mensaje roto o inválido se **registra en el log y se salta** sin bloquear la partición; un error técnico se reintenta 3 veces.
- Los nombres de tópicos y grupos se definen **una sola vez por servicio** en `application.yml`.
- Kafka **no reemplaza** a RabbitMQ: las órdenes, el stock y los correos siguen por RabbitMQ (ver arriba).

## Correr en local (todo en una máquina)

```bash
cd infra
docker compose up -d --build
```

Levanta MySQL, el clúster RabbitMQ (paneles en http://localhost:15672 y :15673) y los servicios.
El clúster **Kafka** va aparte (consume bastante memoria): `cd infra/deploy-ec2/ec2-kafka && docker compose up -d` y, para la consola,
`docker compose -f docker-compose.ui.yml up -d` (http://localhost:8090). Los servicios nuevos de Kafka se levantan con
`cd infra && docker compose -f docker-compose.kafka.yml up -d --build` (ver ese archivo).
Por defecto el correo se **simula** (se escribe en el log de `ms-notificaciones`); para enviarlo de
verdad definir `MAIL_SIMULADO=false`, `GMAIL_USERNAME` y `GMAIL_APP_PASSWORD`.

```bash
curl http://localhost:8081/api/productos      # 200, catálogo
curl -i http://localhost:8082/api/carrito     # 401 sin token
curl http://localhost:8083/api/auth/health    # 200
```

## Desplegar en AWS

Ver [`infra/deploy-ec2/README.md`](infra/deploy-ec2/README.md) y los `docker-compose.yml` de cada
`infra/deploy-ec2/ec2-*`. El API Gateway se define en
[`infra/api-gateway/openapi.yaml`](infra/api-gateway/openapi.yaml).

## Pruebas

```bash
cd ms-productos && mvn verify   # (idem el resto de los servicios)
```

Con Docker, sin instalar Maven:
`docker run --rm -v "$PWD/ms-orden:/app" -v m2cache:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B test`

## Configuración (variables de entorno)

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | Conexión MySQL |
| `AZURE_ISSUER` / `AZURE_AUDIENCE` / `AZURE_JWK_SET_URI` | IDaaS (tenant JOSECAMPOS) |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origen(es) del frontend |
| `APP_ROLES_ADMIN` / `APP_ROLES_VENDEDOR` | Emails → rol (fallback si no hay App Roles) |
| `SERVICES_PRODUCTOS_BASE_URL` / `SERVICES_ORDENES_BASE_URL` | (solo ms-carrito) URLs de ms-productos y ms-orden |
| `SPRING_RABBITMQ_ADDRESSES` | Nodos de RabbitMQ `host:5672,host:5673` (o `HOST`/`PORT` para un solo nodo) |
| `SPRING_RABBITMQ_USERNAME` / `PASSWORD` | Credenciales de RabbitMQ |
| `MESSAGING_RETRY_MAX_ATTEMPTS` / `MESSAGING_RETRY_BACKOFF_MS` | Reintentos antes de la DLQ (3 / 2000) |
| `GMAIL_USERNAME` / `GMAIL_APP_PASSWORD` | (ms-notificaciones) cuenta que envía los correos |
| `MAIL_SIMULADO` | (ms-notificaciones) `true` = el correo solo se escribe en el log |
| `STOCK_ALERT_EMAIL` / `STOCK_UMBRAL_BAJO` | Destino de la alerta de stock bajo / umbral (5) |

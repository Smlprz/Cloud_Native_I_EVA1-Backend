# Despliegue: una EC2 por microservicio

El profesor pide **una instancia EC2 por microservicio**. Aquí está la topología y
los pasos. (Para desarrollo local sigue estando `infra/docker-compose.yml`, que
levanta los 3 en una sola máquina.)

```
                         AWS API Gateway (gzlei5wlz3)
                 /productos/*      /carrito/*        /auth/*
                     |                 |                |
        ┌────────────▼──────┐  ┌───────▼───────┐  ┌─────▼────────┐
        │  EC2 #1 PRODUCTOS  │  │ EC2 #2 CARRITO│  │ EC2 #3 AUTH  │
        │  ms-productos :8081│  │ ms-carrito    │  │ ms-auth :8083│
        │  + MySQL   :3307   │  │        :8082  │  │              │
        └────────┬──────────┘  └───────┬───────┘  └─────┬────────┘
                 │  productos_db        │ carrito_db      │ auth_db
                 └──────────────────────┴─────────────────┘
                          (todas en la MySQL de la EC2 #1)
```

- **EC2 #1** aloja además el contenedor **MySQL** con los 3 esquemas
  (`productos_db`, `carrito_db`, `auth_db`).
- **EC2 #2 y #3** no tienen BD propia: se conectan por red a
  `EC2#1:3307`.

## Security Groups

| EC2 | Puertos de entrada a abrir |
|---|---|
| #1 productos | `8081` (API), `3307` (MySQL, para #2 y #3), `22` (SSH) |
| #2 carrito | `8082`, `22` |
| #3 auth | `8083`, `22` |

> Ideal: que #2 y #3 accedan a #1 por su **IP privada** del VPC (más seguro y la IP
> privada no cambia). Si usan IP pública, hay que abrir `3307` a `0.0.0.0/0`.

## Pasos en cada EC2

Requisitos (una vez): Docker + plugin compose (ver `docs/GUIA-COMPANERO.md`), y un
swapfile de 2 GB si la instancia es `t3.micro`.

**EC2 #1 — productos + MySQL**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-productos
sudo docker compose up -d --build
```

**EC2 #2 — carrito**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-carrito
PRODUCTOS_EC2_HOST=<ip-privada-o-publica-de-EC2-1> sudo -E docker compose up -d --build
```

**EC2 #3 — auth**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-auth
PRODUCTOS_EC2_HOST=<ip-privada-o-publica-de-EC2-1> sudo -E docker compose up -d --build
```

## IPs actuales

| EC2 | IP | Puertos |
|---|---|---|
| #1 productos + MySQL | `3.225.255.55` | 8081 (API), 3307 (MySQL) |
| #2 carrito | `44.193.187.57` | 8082 |
| #3 auth (**Elastic IP**, fija) | `35.170.38.245` | 8083 |

En #2 y #3, `PRODUCTOS_EC2_HOST=3.225.255.55`.

## API Gateway

`infra/api-gateway/openapi.yaml` ya tiene esas 3 IPs. Para aplicar:

```bash
aws apigatewayv2 reimport-api --api-id gzlei5wlz3 --body file://openapi.yaml --region us-east-1
```

Si #1 o #2 se reinician sin Elastic IP su IP cambia → editar `openapi.yaml` y
re-importar. #3 ya tiene Elastic IP.

## Nota sobre `auth_db`

El script `infra/mysql/init/01-init-databases.sql` crea las 3 bases, pero **solo la
primera vez** que se crea el volumen de MySQL. Si la EC2 #1 ya tenía MySQL con datos,
crear la base a mano una vez:
```bash
sudo docker exec -i pedidos360-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4; GRANT ALL PRIVILEGES ON auth_db.* TO 'pedidos'@'%'; FLUSH PRIVILEGES;"
```

---

# Servicios con RabbitMQ (orden, notificaciones, reseñas, rabbit-admin) y el clúster

Además de las 3 EC2 de arriba, hay un `docker-compose.yml` por servicio en `ec2-*/`:

| Carpeta | Servicio | Puerto | Necesita |
|---|---|---|---|
| `ec2-rabbitmq` | **Clúster RabbitMQ de 2 nodos** | 5672/5673 (AMQP), 15672/15673 (panel) | — |
| `ec2-rabbit-admin` | `ms-rabbit-admin` | 8087 | `RABBITMQ_EC2_HOST` |
| `ec2-orden` | `ms-orden` | 8085 | `PRODUCTOS_EC2_HOST`, `RABBITMQ_EC2_HOST`, esquema `orden_db` |
| `ec2-notificaciones` | `ms-notificaciones` | 8084 | `RABBITMQ_EC2_HOST`, `GMAIL_USERNAME`/`GMAIL_APP_PASSWORD` (o `MAIL_SIMULADO=true`) |
| `ec2-resenas` | `ms-resenas` | 8086 | `PRODUCTOS_EC2_HOST`, esquema `resenas_db` |
| `ec2-productos` | `ms-productos` | 8081 | ahora también `RABBITMQ_EC2_HOST` (consume `stock.descontar`) |

## Orden de arranque recomendado
1. **`ec2-rabbitmq`**: levantar el clúster y comprobarlo (los demás se conectan a él).
2. **`ec2-productos`** (con `RABBITMQ_EC2_HOST`): declara la cola `stock.descontar` y su DLQ.
3. **`ec2-notificaciones`**: declara las colas `orden.creada` y `stock.bajo` con sus DLQ.
4. **`ec2-orden`**, **`ec2-carrito`**, el resto. *(ms-orden solo publica: si arranca antes que los
   consumidores, los mensajes no tienen cola y se pierden — quedan registrados como "NO ENRUTADO" en su log.)*

## Clúster RabbitMQ
```bash
cd infra/deploy-ec2/ec2-rabbitmq
RABBITMQ_USER=pedidos RABBITMQ_PASSWORD=<clave> RABBITMQ_ERLANG_COOKIE=<texto-largo> sudo -E docker compose up -d
sudo docker exec rabbitmq-1 rabbitmqctl cluster_status        # deben verse rabbit@rabbitmq-1 y rabbit@rabbitmq-2
```
Los servicios usan `SPRING_RABBITMQ_ADDRESSES=<ip>:5672,<ip>:5673` (ya lo arman sus compose a partir de
`RABBITMQ_EC2_HOST`). Si cae un nodo, el cliente se reconecta al otro; las colas están replicadas por la
política `ha-all` (la aplica la tarea `rabbitmq-setup`, que termina en `Exited (0)`).

**Security Group de la EC2 de RabbitMQ:** 22 y 15672/15673 solo desde tu IP; 5672/5673 solo desde las EC2 de los
servicios (o el rango de la VPC).

## Esquemas de MySQL nuevos
El script `infra/mysql/init/01-init-databases.sql` ya crea `orden_db` y `resenas_db`, pero **solo la primera vez**
que se crea el volumen de MySQL. Si la EC2 de productos ya tenía datos:
```bash
sudo docker exec -i pedidos360-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS orden_db CHARACTER SET utf8mb4; CREATE DATABASE IF NOT EXISTS resenas_db CHARACTER SET utf8mb4; GRANT ALL PRIVILEGES ON orden_db.* TO 'pedidos'@'%'; GRANT ALL PRIVILEGES ON resenas_db.* TO 'pedidos'@'%'; FLUSH PRIVILEGES;"
```
`ms-productos` aplica solo la migración `V3` (tabla `stock_movimientos`) al arrancar.

## Despliegue con `scp` + imágenes de Docker Hub (recomendado)
Cada `ec2-*/docker-compose.yml` usa la imagen `tukionbush/pedidos360-ms-<servicio>:2.0` (se puede cambiar con
`DOCKERHUB_USER` / `IMAGE_TAG`), así que la EC2 solo descarga la imagen; no hace falta clonar el repo ni compilar.
```bash
# 1) desde tu PC: copiar la carpeta del servicio
scp -i "C:\Users\elope\.ssh\pedidos360-key.pem" -r infra\deploy-ec2\ec2-<servicio> ec2-user@<IP>:~/
# 2) en la EC2: crear el .env con las variables que pide ese compose y levantar
cd ~/ec2-<servicio> && sudo docker compose up -d
```
> **ec2-productos:** el `scp` desde Windows deja `mysql-init` sin permiso de lectura para el usuario `mysql`
> del contenedor (`ls: cannot open directory '/docker-entrypoint-initdb.d/': Permission denied`).
> Ejecutar **`chmod -R a+rX ~/ec2-productos/mysql-init`** antes de `docker compose up -d`
> (y `docker compose down -v` si ya se había intentado levantar).


---

# Evaluación 3: Kafka, pagos y analítica

Se suman 3 microservicios (`ms-pago`, `ms-analytics-recommendations`, `ms-kafka-admin`) y el clúster Kafka. RabbitMQ no cambia.

## Plan de 8 instancias (AWS Learner Lab, máximo 9 simultáneas)

| # | Instancia | Contiene | Tipo | Disco | Puertos a abrir |
|---|---|---|---|---|---|
| 1 | `pedidos360-productos` | ms-productos + MySQL (`ec2-productos`) | t3.medium | 20 GB | 8081, 3307 |
| 2 | `pedidos360-carrito-auth` | ms-carrito + ms-auth | t3.small | 20 GB | 8082, 8083 |
| 3 | `pedidos360-orden-pago` | ms-orden + ms-pago | t3.small | 20 GB | 8085, 8088 |
| 4 | `pedidos360-resenas-notif` | ms-resenas + ms-notificaciones | t3.small | 20 GB | 8086, 8084 |
| 5 | `pedidos360-rabbitmq` | RabbitMQ (2 nodos) + ms-rabbit-admin | t3.small | 20 GB | 5672, 5673, 15672, 8087 |
| 6 | `pedidos360-kafka` | 3 Zookeeper + 3 brokers + Kafka-UI + ms-kafka-admin | **t3.medium** | **30 GB** | 9092-9094, 8090, 8091 |
| 7 | `pedidos360-analytics` | ms-analytics-recommendations | t3.small (+ rol IAM) | 20 GB | 8089 |
| 8 | `pedidos360-frontend` | Caddy + React | t3.micro | 20 GB | 80, 443 |

Cada instancia ejecuta la `docker-compose.yml` de su carpeta `ec2-*` (varias carpetas en la misma instancia = varios `docker compose` con proyectos distintos).
Con `JAVA_TOOL_OPTIONS=-Xmx256m` por servicio y 2 GB de swap (el script de arranque ya lo crea) dos servicios caben en una t3.small.
Medir con `docker stats` antes de la demostración.

## Orden de arranque

1. **MySQL + ms-productos** (la base crea `pago_db` en su primer arranque; con un volumen ya existente hay que crearla a mano:
   `docker exec -i pedidos360-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS pago_db; GRANT ALL ON pago_db.* TO 'pedidos'@'%';"`).
2. **RabbitMQ** y luego el resto de los servicios de RabbitMQ (ver más arriba).
3. **Kafka**: `ec2-kafka` → `KAFKA_ADVERTISED_HOST=<ip-privada-de-esa-EC2> sudo -E docker compose up -d`. Esperar a que `kafka-setup` termine con
   `Exited (0)` (crea los 2 tópicos). Luego `docker compose -f docker-compose.ui.yml up -d` y `docker compose -f docker-compose.admin.yml up -d`.
4. **ms-productos y ms-carrito** con `KAFKA_EC2_HOST=<ip-privada-kafka>` (publican eventos de navegación).
5. **ms-pago** (`ORDENES_URL`, `PRODUCTOS_EC2_HOST`, `KAFKA_EC2_HOST`) y **ms-analytics** (`KAFKA_EC2_HOST`, `PRODUCTOS_URL`).
6. **Frontend** con `ANALYTICS_UPSTREAM=<ip-privada-analytics>:8089` (Caddy reenvía el WebSocket `/ws/`).

Usar siempre **IPs privadas** entre instancias (no cambian al reiniciar el lab) y las públicas solo para el navegador y el API Gateway.

## Clúster Kafka

- `KAFKA_ADVERTISED_HOST` = IP privada de la EC2 de Kafka: es la dirección que los brokers anuncian a los clientes de otras máquinas (listener `EXTERNAL`, puertos 9092/9093/9094).
  Los contenedores de la misma red (Kafka-UI, ms-kafka-admin) usan `kafka-1:29092`…
- Verificar: `sudo docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --describe` → los 2 tópicos con `PartitionCount: 3  ReplicationFactor: 3` y `Isr: 1,2,3`.
- Tolerancia a fallos: `sudo docker stop kafka-2` → los tópicos siguen disponibles (`min.insync.replicas=2`); `docker start kafka-2` y vuelve a sincronizar.
- Memoria: los 6 contenedores ocupan ~1,3 GB en total (brokers con heap de 512 MB, Zookeeper de 256 MB).

## DynamoDB

`ms-analytics` crea las tablas `pedidos360_eventos` y `pedidos360_perfiles` al arrancar. En AWS Academy asignar a la instancia el rol
**`LabInstanceProfile`** (EC2 → Acciones → Seguridad → Modificar rol de IAM) y dejar `ANALYTICS_STORE=dynamodb`: no hacen falta claves.

## Rutas nuevas del API Gateway

`/pagos`, `/analytics/recomendaciones|perfil|metricas`, `/kafka/**` (ver `infra/api-gateway/openapi.yaml`, con `REEMPLAZAR_IP_*`).
Si la cuenta del laboratorio no permite importar el OpenAPI (`PUT /apis` denegado), crear las rutas a mano: una ruta `ANY /pagos`
con el autorizador `azureJwt` y su integración HTTP, **más una ruta `OPTIONS` sin autorizador** (si no, el preflight de CORS responde 401).
El WebSocket **no** pasa por el gateway: lo reenvía el Caddy del frontend.

## Variables nuevas

| Variable | Servicio | Descripción |
|---|---|---|
| `KAFKA_EC2_HOST` | productos, carrito, pago, analytics | IP privada de la EC2 de Kafka (se arman los 3 bootstrap servers) |
| `KAFKA_ADVERTISED_HOST` | cluster Kafka | IP privada anunciada a los clientes externos |
| `CARD_HASH_PEPPER` | pago | secreto para el hash de tarjetas (no cambiarlo después) |
| `ANALYTICS_STORE` | analytics | `dynamodb` (nube) o `memory` |
| `ANALYTICS_UPSTREAM` | frontend (Caddy) | `<ip-privada-analytics>:8089` |

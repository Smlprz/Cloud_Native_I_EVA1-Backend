# ms-productos — Microservicio de Catálogo (Pedidos360)

Microservicio Spring Boot que expone el catálogo de productos de la tienda digital
Pedidos360. Forma parte de la arquitectura Cloud-Native multi-nube:

- **Azure AD (Entra ID)** emite los tokens JWT (IDaaS).
- **AWS API Gateway** es el único punto de entrada y valida el token en el borde.
- Este microservicio **vuelve a validar el JWT** (firma, vigencia, `issuer`, `audience`)
  actuando como Resource Server — defensa en profundidad.

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/productos` | Público (opcional `?categoria=` o `?nombre=`) |
| GET | `/api/productos/{id}` | Público |
| POST | `/api/productos` | Rol `VENDEDOR` o `ADMIN` |
| PUT | `/api/productos/{id}` | Rol `VENDEDOR` o `ADMIN` |
| DELETE | `/api/productos/{id}` | Rol `ADMIN` |
| GET | `/actuator/health` | Público (health check para contenedores) |

Los roles provienen del claim `roles` (App Roles de Azure) y los permisos del claim `scp`.

## Configuración (variables de entorno)

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC de MySQL | `jdbc:mysql://localhost:3306/productos_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `pedidos` |
| `SPRING_DATASOURCE_PASSWORD` | Password BD | `pedidos` |
| `AZURE_JWK_SET_URI` | JWKS de Azure | placeholder — ver `docs/AZURE-AD-SETUP.md` |
| `AZURE_ISSUER` | `issuer` esperado del token | placeholder |
| `AZURE_AUDIENCE` | `audience` esperada (App ID URI) | placeholder |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origen del frontend permitido | `http://localhost:4200` |

## Ejecutar en local

```bash
# 1. Levantar MySQL (desde la carpeta infra/ del repo raíz)
docker compose up -d mysql

# 2. Compilar y probar
mvn clean verify

# 3. Arrancar
mvn spring-boot:run
```

El esquema y los datos semilla los crea **Flyway** automáticamente
(`src/main/resources/db/migration`).

## Pruebas

```bash
mvn test
```

- `ProductRepositoryTest` — persistencia con base H2 en memoria.
- `ProductControllerTest` — catálogo público + rechazo de escrituras sin token (401)
  o sin rol (403) y alta correcta con rol `VENDEDOR` (201).


## Mensajería (RabbitMQ)

**Consume `stock.descontar`** (cola `stock.descontar`, exchange direct `pedidos.exchange`), que publica
`ms-orden` al crear una orden con ítems, y descuenta el stock (`StockService`).

- **Todo o nada** por orden (una transacción) y **idempotente**: la tabla `stock_movimientos`
  (migración `V3`) tiene `orden_id` único, así un mensaje duplicado no descuenta dos veces.
- Producto inexistente o sin stock suficiente → error de negocio: no se reintenta, el mensaje va a
  `stock.descontar.dlq` y queda en el log. Errores técnicos (BD caída) → hasta 3 reintentos y luego DLQ.
- **Publica `stock.bajo`** en el exchange **topic** `pedidos.eventos` cuando un descuento deja un producto con
  stock ≤ `app.stock.umbral-bajo` (5 por defecto, `STOCK_UMBRAL_BAJO`). `ms-notificaciones` avisa por correo.
- Estructura: `messaging.config` (propiedades, topología, `RabbitTemplate`), `messaging.stock`
  (consumidor y publicador), `messaging.support` (ACK/reintento/DLQ y parseo con validación).
- Variables: `SPRING_RABBITMQ_ADDRESSES`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`,
  `MESSAGING_RETRY_MAX_ATTEMPTS`, `MESSAGING_RETRY_BACKOFF_MS`, `STOCK_UMBRAL_BAJO`.

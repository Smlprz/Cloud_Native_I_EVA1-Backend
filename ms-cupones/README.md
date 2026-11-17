# ms-cupones

Microservicio libre de la Evaluación 3: **cupones de descuento** (puerto 8092 en la EC2, 8080 dentro del contenedor). Los administradores crean
cupones; los clientes los aplican en el carrito y **el total del pedido baja de verdad**. Al usarse, publica `CUPON_APLICADO` en Kafka
(`eventos.navegacion`) y el dashboard lo cuenta. Base de datos: MySQL `cupones_db` (Flyway).
Seguridad: JWT de Azure AD, rol CLIENTE/ADMIN y scope `cupones.*` (o `carrito.*` mientras `cupones.*` no exista en Azure).

## Cómo se usa en una compra
1. En el carrito el cliente escribe el código y pulsa **Aplicar** → `POST /api/cupones/validar` calcula el descuento **sin consumir** el cupón.
2. Al **Confirmar compra**, `ms-carrito` llama a `POST /api/cupones/aplicar` reenviando el JWT del usuario: el cupón se consume y el total del
   pedido queda con el descuento (el pedido guarda `descuento` y `cuponCodigo`). Ese total es el que viaja a `ms-orden`, a `ms-pago` y al correo.
3. Si el cupón no sirve, el checkout se **rechaza con el motivo** antes de guardar nada: el carrito sigue abierto y se puede reintentar sin cupón.

## Reglas
Un cupón se puede usar si: existe y está activo · no venció · la compra alcanza el **monto mínimo** · aún quedan usos · el usuario no lo usó antes.

| Tipo | Descuento |
|---|---|
| `PORCENTAJE` | `monto × valor / 100`, redondeado a 2 decimales (máximo 100 %) |
| `MONTO_FIJO` | `valor` pesos |

El descuento **nunca supera el monto** (el total no queda negativo). Los códigos se guardan en mayúsculas y no distinguen mayúsculas al usarlos.

## Concurrencia (que no se pase de los usos)
- `usosMaximos`: el uso se descuenta con un **`UPDATE` condicional atómico** (`... WHERE usos_actuales < usos_maximos`), así que con muchas
  solicitudes simultáneas nunca se supera el máximo. Hay una prueba con 12 usuarios a la vez sobre un cupón de 3 usos: exactamente 3 lo consiguen.
- Un usuario solo puede usar un cupón una vez: lo garantiza la **restricción única `(cupon_id, usuario_id)`** de la base, aunque lleguen dos
  solicitudes a la vez (la segunda revierte su transacción, incluido el contador).

## Endpoints (`/api/cupones`)
| Método | Ruta | Quién | Qué hace |
|---|---|---|---|
| POST | `/validar` | CLIENTE/ADMIN | `{"codigo","monto"}` → descuento y total, sin consumir |
| POST | `/aplicar` | CLIENTE/ADMIN | igual, pero **consume** el cupón (lo llama ms-carrito) |
| GET | `/` | ADMIN | lista los cupones con sus usos |
| POST | `/` | ADMIN | `{"codigo","tipo","valor","montoMinimo?","venceEn?","usosMaximos?"}` |
| PUT | `/{id}/activo` | ADMIN | `{"activo":true\|false}` |

Errores (RFC 7807, el motivo viene en `detail`): **404** no existe o está desactivado · **400** vencido, monto mínimo no alcanzado o dato inválido ·
**409** agotado, ya usado o código repetido.

## Evento publicado (`eventos.navegacion`)
Mismo contrato que `ms-productos` y `ms-carrito`, con un campo opcional más (`detalle`) que los consumidores ignoran si no lo conocen:
```json
{"eventId":"…","tipo":"CUPON_APLICADO","usuarioId":"…","productoId":null,"precio":6598.00,"origen":"ms-cupones",
 "timestamp":"2026-11-17T12:00:00Z","detalle":"cupon VERANO10: -$6598.00"}
```
Clave = usuario; productor `acks=all`, idempotente. Si Kafka no responde el cupón **igual se aplica** (solo queda un aviso en el log).

## Variables de entorno
`SPRING_DATASOURCE_*`, `KAFKA_BOOTSTRAP_SERVERS`, `APP_ROLES_ADMIN`, `AZURE_*`. En `ms-carrito`: `CUPONES_URL=http://<ip-privada>:8092` (vacío = sin cupones).

## Limitación conocida
El cupón se consume antes de guardar el pedido. Si la base de datos de carrito fallara justo después, el cupón quedaría usado sin pedido. En ese caso
un administrador puede crear otro cupón; se dejó así por simplicidad (no hay compensación automática).

## Probar
```bash
mvn test   # reglas, concurrencia, API, migraciones (restricciones unicas) y el evento contra Kafka real
```

## Despliegue
`infra/deploy-ec2/ec2-cupones/docker-compose.yml`. Rutas en el gateway: `/cupones`, `/cupones/validar`, `/cupones/{id}/activo`.

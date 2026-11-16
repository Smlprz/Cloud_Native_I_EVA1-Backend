# ms-analytics-recommendations

Analitica y recomendaciones sobre **Kafka** (puerto 8089 en la EC2, 8080 dentro del contenedor). Consume los eventos de navegacion
y de pagos, arma el perfil de cada usuario, recomienda productos parecidos y calcula **metricas en tiempo real** que el dashboard recibe
por **WebSocket**. Historial y perfiles en **Amazon DynamoDB**.

## Arquitectura
```
ms-productos --PRODUCTO_VISTO-------------> [eventos.navegacion] --> grupo analytics-recomendaciones --> perfiles (DynamoDB) --> /api/analytics/recomendaciones
ms-carrito   --ITEM_AGREGADO/ELIMINADO/                           --> grupo analytics-metricas        --> metricas en memoria --> WebSocket /ws/metricas
               COMPRA_REALIZADA---------->  [eventos.navegacion]
ms-pago      --PAGO_APROBADO/RECHAZADO/
               FRAUDE_BLOQUEADO---------->  [eventos.pagos]       --> grupo analytics-metricas
```
- Cada topico tiene **3 particiones** y **3 replicas**. Cada listener levanta **3 consumidores** (`concurrency`) que se reparten las
  particiones; la clave del mensaje es el usuario, asi sus eventos llegan **en orden** a un solo consumidor (sin carreras sobre su perfil).
- **Dos consumer groups** leen el mismo topico de forma independiente (cada uno con su offset y su lag): recomendaciones y metricas.
- Los consumidores (`kafka/`) solo reciben, validan y delegan; la logica esta en `service/`.

## Eventos
Contrato identico al que publican los productores (ver cabeceras de `event/NavegacionEvent` y `event/PagoEvent`). Se deserializan con DTO
tipados, **ignorando la cabecera `__TypeId__` y los campos desconocidos**: si un productor agrega un campo, este servicio sigue funcionando.

## Perfil y recomendaciones
Puntaje por categoria: ver un producto = 1, agregarlo al carrito = 3, comprarlo = 5, quitarlo = -1 (nunca baja de 0). Se recuerdan los
ultimos 10 productos vistos y los comprados. Los visitantes sin sesion (`anonimo`) no tienen perfil.

`RecomendacionService` puntua cada producto del catalogo (descargado de ms-productos, publico, con cache de 60 s):
afinidad de su categoria + 2 si es de la categoria que esta mirando ahora + hasta 0,5 por popularidad − 0,5 si ya lo vio.
Descarta los sin stock, el actual y los ya comprados. Un usuario sin historial recibe los **mas vistos**.

## Manejo de errores (consumidores)
- JSON roto o evento invalido (tipo desconocido, falta un dato): **no se reintenta**; se registra en el log con su topico, particion,
  offset y contenido, se cuenta en la metrica `mensajesDescartados` y **se sigue con el siguiente** (no bloquea la particion).
- Error tecnico (por ejemplo DynamoDB no responde): hasta 3 reintentos con espera y luego el mismo descarte registrado.
- Duplicados (Kafka entrega "al menos una vez"): el `PutItem` del historial es condicional y las metricas recuerdan los ultimos ids.
- Offsets confirmados registro a registro (`AckMode.RECORD`): si el servicio cae, retoma donde quedo.

## DynamoDB
Dos tablas (se crean solas al arrancar, modo on-demand): `pedidos360_eventos` (PK `usuarioId`, SK `<instante>#<eventId>`) y
`pedidos360_perfiles` (PK `usuarioId`). `ANALYTICS_STORE=dynamodb` en la nube (rol `LabInstanceProfile` de la EC2, sin claves);
`memory` para desarrollo. Con `DYNAMODB_ENDPOINT=http://localhost:8000` usa DynamoDB Local.

## API REST (`/api/analytics`) — JWT con scope `productos.read` o `carrito.read`
`GET /recomendaciones?productoId=&limite=` · `GET /perfil` · `GET /metricas` (solo ADMIN).

## WebSocket (`/ws/metricas`) — solo ADMIN
`wss://<sitio>/ws/metricas?token=<JWT>`. El navegador no puede enviar `Authorization` en un WebSocket, asi que el JWT va en la URL y se
valida **antes** de abrir la conexion (firma, vigencia, emisor, audiencia y rol ADMIN): 401 sin token valido, 403 si no es ADMIN. Envia la foto
de las metricas al conectar y cada vez que cambian (maximo 1 por segundo; refresca cada 5 s aunque no haya cambios). Metricas: clics totales,
clics del ultimo minuto, usuarios activos, productos mas vistos (top 5), clics por tipo, pagos aprobados/rechazados/bloqueados, ultimas alertas de
fraude y mensajes descartados. El API Gateway HTTP no soporta WebSockets: el Caddy del frontend reenvia `/ws/` a este servicio.

## Variables de entorno
`KAFKA_BOOTSTRAP_SERVERS`, `ANALYTICS_STORE`, `AWS_REGION`, `DYNAMODB_ENDPOINT`, `PRODUCTOS_URL`, `KAFKA_CONSUMER_CONCURRENCY`,
`KAFKA_LISTENERS_ENABLED`, `APP_ROLES_ADMIN`, `AZURE_*`.

## Probar
```bash
mvn test                                   # perfiles, recomendaciones, metricas, consumidores contra Kafka real, API y WebSocket
# prueba contra DynamoDB (opcional): docker run -p 8000:8000 amazon/dynamodb-local  y  DYNAMODB_ENDPOINT=http://localhost:8000 mvn test -Dtest=DynamoAnalyticsStoreIT
```

## Despliegue
`infra/deploy-ec2/ec2-analytics/docker-compose.yml`. Rutas en el gateway: `/analytics/**`.

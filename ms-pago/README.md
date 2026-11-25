# ms-pago

Pagos y antifraude (puerto 8088 en la EC2, 8080 dentro del contenedor). Procesa el pago **simulado** de una orden,
aplica la regla antifraude y publica el resultado en el topico Kafka `eventos.pagos`. Base de datos: MySQL `pago_db` (Flyway).
Seguridad: JWT de Azure AD, rol CLIENTE/ADMIN y scope `pagos.*` (o `ordenes.*` / `carrito.*` mientras `pagos.*` no exista en Azure).

## Flujo de `POST /api/pagos`
1. Consulta la orden en **ms-orden** reenviando el JWT del usuario: el **monto sale de la orden** (no de lo que envie el cliente)
   y solo se puede pagar una orden propia (404 si no existe o no es suya).
2. Registra el intento en el **detector antifraude**.
3. Si no esta bloqueado: valida la tarjeta (Luhn, vencimiento) y aprueba o rechaza.
4. Guarda el pago y publica el evento en Kafka.

| Resultado | HTTP | Cuando |
|---|---|---|
| `APROBADO` | 201 | tarjeta valida |
| `RECHAZADO` | 402 | numero invalido, tarjeta vencida o tarjeta de prueba terminada en `0002` (fondos insuficientes) |
| `BLOQUEADO_FRAUDE` | 429 | la misma tarjeta se uso **mas de 3 veces en 10 segundos** |
| (error) | 409 | la orden ya estaba pagada · 404 orden inexistente · 400 datos invalidos · 502 ms-orden no responde |

## Regla antifraude (`DetectorFraude`)
Ventana deslizante por tarjeta: si en los ultimos `ANTIFRAUDE_VENTANA_SEGUNDOS` (10) hay mas de `ANTIFRAUDE_MAX_INTENTOS` (3)
intentos con la misma tarjeta, el cuarto se **bloquea**. Se cuentan todos los intentos (aprobados, rechazados y bloqueados): quien
sigue insistiendo permanece bloqueado hasta que pasa la ventana. Al bloquear se guarda una **alerta** en `alertas_fraude`, se deja un
`WARN` en el log y se publica `FRAUDE_BLOQUEADO` en Kafka. La tarjeta se identifica por su hash; el estado vive en memoria de la instancia.

## Seguridad de los datos de tarjeta
- El **numero completo y el CVV no se guardan nunca**: solo los ultimos 4 digitos y un hash SHA-256 con *pepper* (`CARD_HASH_PEPPER`).
- `PagoRequest.toString()` no imprime la tarjeta y los errores de validacion no devuelven el valor enviado.

## Evento publicado (`eventos.pagos`)
Clave = `usuarioId` (los eventos de un usuario van a la misma particion, en orden). JSON sin cabecera de tipo Java:
```json
{"eventId":"…","tipo":"PAGO_APROBADO|PAGO_RECHAZADO|FRAUDE_BLOQUEADO","ordenId":42,"usuarioId":"…",
 "monto":65980.00,"tarjetaUltimos4":"1111","motivo":null,"timestamp":"2026-11-16T12:00:00Z"}
```
Productor: `acks=all`, idempotente, `max.block.ms` corto. Si Kafka no responde el pago **no se deshace** (queda en la base y en el log).

## Endpoints (`/api/pagos`)
`POST /` pagar · `GET /?ordenId=` mis pagos · `GET /alertas-fraude` ultimas 50 alertas (solo ADMIN).

## Variables de entorno
`SPRING_DATASOURCE_*`, `ORDENES_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `ANTIFRAUDE_MAX_INTENTOS`, `ANTIFRAUDE_VENTANA_SEGUNDOS`,
`CARD_HASH_PEPPER`, `APP_ROLES_ADMIN`, `AZURE_*`.

## Probar
```bash
mvn test   # antifraude con reloj controlado y concurrencia, tarjetas, flujo con H2, API, migraciones y evento contra Kafka real
```
Para la demo del antifraude: pagar una orden con la tarjeta `4000 0000 0018 0002` (siempre rechazada) 4 veces seguidas: la cuarta se bloquea.

## Despliegue
`infra/deploy-ec2/ec2-pago/docker-compose.yml`. Ruta en el gateway: `/pagos`.

## Webpay Plus

Ademas del pago simulado, la orden se puede pagar con Webpay (ambiente de **integracion** de Transbank).

| Metodo | Ruta | Que hace |
|---|---|---|
| POST | `/api/pagos/webpay/iniciar` `{ordenId}` | Antifraude, crea la transaccion y devuelve `url` y `token` |
| POST | `/api/pagos/webpay/confirmar` `{token}` | Commit al volver de Webpay; registra el pago y publica el evento |
| POST | `/api/pagos/webpay/anular` `{token}` | El usuario anulo en Webpay |

**Antifraude en Webpay:** la tarjeta se escribe en Transbank y el comercio nunca la ve, por eso se cuenta por
**id de usuario** (mas de 3 intentos en 10 segundos: 429, alerta y evento en `eventos.pagos`).
El pago simulado mantiene la regla por tarjeta.

Variables: `WEBPAY_RETURN_URL` (`<frontend>/pago/retorno`), `WEBPAY_BASE_URL`, `WEBPAY_COMMERCE_CODE`,
`WEBPAY_API_KEY` (por defecto, las credenciales publicas de prueba de Transbank).

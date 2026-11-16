# ms-kafka-admin

Microservicio administrador de Kafka (puerto 8091 en la EC2, 8080 dentro del contenedor). Crea, lista, modifica y
elimina **topicos** y consulta los **consumer groups** (offsets y lag) usando el `AdminClient` de Kafka. No usa base de datos.
Seguridad: JWT de Azure AD (app `pedidos360-api`), rol **ADMIN** y scope `kafka.admin`
(o `carrito.write` mientras `kafka.admin` no exista en Azure), igual que el administrador de RabbitMQ.

## Endpoints (`/api/kafka`)
| Metodo | Ruta | Body / resultado |
|---|---|---|
| GET | `/cluster` | id del cluster, controlador y brokers |
| GET | `/topics` | nombres de los topicos |
| GET | `/topics/{name}` | particiones, replicas, ISR por particion y configuracion explicita |
| POST | `/topics` | `{"name":"x","partitions":3,"replicationFactor":3,"config":{"retention.ms":"86400000","cleanup.policy":"delete"}}` |
| PUT | `/topics/{name}/config` | `{"config":{"retention.ms":"3600000"}}` |
| PUT | `/topics/{name}/partitions` | `{"total":6}` (solo se pueden **aumentar**) |
| DELETE | `/topics/{name}` | los topicos del sistema estan protegidos |
| GET | `/consumer-groups` | ids de los grupos |
| GET | `/consumer-groups/{groupId}` | estado, miembros, offset confirmado, offset final y **lag** por particion |
| DELETE | `/consumer-groups/{groupId}` | solo grupos sin miembros activos |

`partitions` y `replicationFactor` son opcionales al crear: por defecto **3 y 3** (como el cluster).

## Configuraciones de topico permitidas
`retention.ms`, `retention.bytes`, `cleanup.policy`, `min.insync.replicas`, `segment.bytes`, `max.message.bytes`,
`compression.type`. Cualquier otra se rechaza con 400 (lista cerrada: evita tocar parametros peligrosos).

## Validacion y errores (RFC 7807)
- Nombres de 1 a 249 caracteres: letras, numeros y `. _ -`, empezando con letra o numero.
- **400** dato invalido (nombre, particiones, replicas mayores que los brokers, configuracion no permitida, reducir particiones).
- **404** topico o grupo inexistente · **409** el topico ya existe, es del sistema (`eventos.navegacion`, `eventos.pagos`) o el
  grupo tiene miembros · **503** el cluster no responde (tiempos cortos: contesta en segundos, no se cuelga).
- La logica esta encapsulada en `KafkaAdminService`; el controlador solo valida y delega.

## Variables de entorno
`KAFKA_BOOTSTRAP_SERVERS` (`kafka-1:29092,kafka-2:29092,kafka-3:29092` dentro de la red docker), `KAFKA_PROTECTED_TOPICS`,
`APP_ROLES_ADMIN` (correos con rol ADMIN), `AZURE_*`.

## Probar
```bash
mvn test        # incluye pruebas contra un broker Kafka real (embebido): crear, modificar, eliminar y medir lag
```

## Despliegue
`infra/deploy-ec2/ec2-kafka/docker-compose.admin.yml` (se une a la red del cluster). Ruta en el gateway: `/kafka/**`.

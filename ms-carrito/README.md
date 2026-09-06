# ms-carrito — Microservicio de Carrito / Pedidos (Pedidos360)

Microservicio Spring Boot que gestiona el carrito privado de cada usuario y el
registro de pedidos. Es la parte **protegida** de la arquitectura: exige token
válido de Azure AD con **rol + scope** correctos.

- **Azure AD (Entra ID)** emite el JWT (IDaaS).
- **AWS API Gateway** valida el token en el borde y enruta `/carrito/*` aquí.
- Este microservicio **revalida el JWT** (firma, vigencia, `issuer`, `audience`) y
  aplica autorización fina con `@PreAuthorize`.
- Para resolver nombre/precio/stock consulta a **ms-productos** por HTTP,
  reenviando el mismo token del usuario.

## Endpoints (todos requieren autenticación)

| Método | Ruta | Autorización |
|---|---|---|
| GET | `/api/carrito` | `SCOPE_carrito.read` + rol `CLIENTE`/`ADMIN` |
| POST | `/api/carrito/items` | `SCOPE_carrito.write` + rol `CLIENTE`/`ADMIN` |
| PUT | `/api/carrito/items/{itemId}` | `SCOPE_carrito.write` + rol `CLIENTE`/`ADMIN` |
| DELETE | `/api/carrito/items/{itemId}` | `SCOPE_carrito.write` + rol `CLIENTE`/`ADMIN` |
| DELETE | `/api/carrito` | `SCOPE_carrito.write` + rol `CLIENTE`/`ADMIN` |
| POST | `/api/carrito/checkout` | `SCOPE_carrito.write` + rol `CLIENTE`/`ADMIN` |
| GET | `/api/carrito/pedidos` | `SCOPE_carrito.read` + rol `CLIENTE`/`ADMIN` |
| GET | `/actuator/health` | Público |

El usuario se identifica por el claim `oid` (o `sub`) del token: cada quien solo
ve y modifica su propio carrito.

## Configuración (variables de entorno)

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC de MySQL | `jdbc:mysql://localhost:3306/carrito_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `pedidos` |
| `SPRING_DATASOURCE_PASSWORD` | Password BD | `pedidos` |
| `SERVICES_PRODUCTOS_BASE_URL` | URL base de ms-productos | `http://localhost:8081` |
| `AZURE_JWK_SET_URI` | JWKS de Azure | placeholder — ver `docs/AZURE-AD-SETUP.md` |
| `AZURE_ISSUER` | `issuer` esperado del token | placeholder |
| `AZURE_AUDIENCE` | `audience` esperada (App ID URI) | placeholder |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origen del frontend permitido | `http://localhost:4200` |

## Ejecutar en local

```bash
# 1. Levantar MySQL + ms-productos (desde infra/ del repo raíz)
docker compose up -d mysql ms-productos

# 2. Compilar y probar
mvn clean verify

# 3. Arrancar
mvn spring-boot:run
```

Flyway crea el esquema (`carritos`, `carrito_items`, `pedidos`, `pedido_items`)
automáticamente.

## Pruebas

```bash
mvn test
```

- `CarritoServiceTest` — alta de ítem, control de stock y checkout con total correcto.
- `CarritoControllerSecurityTest` — 401 sin token, 403 sin scope/rol, 200 con
  `SCOPE_carrito.read` + `ROLE_CLIENTE`.

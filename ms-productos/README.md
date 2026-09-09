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

# Pedidos360 — Backend (Cloud Native I, EVA1)

Backend de microservicios de la tienda **Pedidos360**. Identidad delegada a
**Azure AD**, tráfico perimetral por **AWS API Gateway**, microservicios Spring Boot
en contenedores sobre **EC2** con **MySQL**.

## Microservicios

| Servicio | Puerto | BD | Rol |
|---|---|---|---|
| [`ms-productos`](ms-productos/) | 8081 | `productos_db` | Catálogo. GET público; escrituras con rol. |
| [`ms-carrito`](ms-carrito/) | 8082 | `carrito_db` | Carrito y pedidos. Todo con scope `carrito.*` + rol. |
| [`ms-auth`](ms-auth/) | 8083 | `auth_db` | Identidad (BFF): valida el JWT de Azure, expone el perfil y **audita los accesos**. |

Los tres validan el JWT de Azure (firma vía JWKS + vigencia + `issuer` + `audience`).
Roles: del claim `roles` si existe (normalizado `Administrador`→ADMIN…), o por email
(`APP_ROLES_ADMIN` / `APP_ROLES_VENDEDOR`), con `CLIENTE` por defecto.

## Correr en local (todo en una máquina)

```bash
cd infra
docker compose up -d --build
```

- `ms-productos` → http://localhost:8081
- `ms-carrito`  → http://localhost:8082
- `ms-auth`     → http://localhost:8083
- MySQL         → localhost:3307 (3 esquemas)

```bash
curl http://localhost:8081/api/productos      # 200, catálogo
curl -i http://localhost:8082/api/carrito     # 401 sin token
curl http://localhost:8083/api/auth/health    # 200
```

## Desplegar en AWS (una EC2 por microservicio)

Ver [`infra/deploy-ec2/README.md`](infra/deploy-ec2/README.md): topología de 3
instancias, Security Groups, y los `docker-compose.yml` por EC2.

El API Gateway se define en [`infra/api-gateway/openapi.yaml`](infra/api-gateway/openapi.yaml)
(rutas `/productos/*`, `/carrito/*`, `/auth/*`).

## Pruebas

```bash
cd ms-productos && mvn verify   # (idem ms-carrito, ms-auth)
```

## Configuración (variables de entorno)

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | Conexión MySQL |
| `AZURE_ISSUER` / `AZURE_AUDIENCE` / `AZURE_JWK_SET_URI` | IDaaS (tenant JOSECAMPOS) |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origen(es) del frontend |
| `APP_ROLES_ADMIN` / `APP_ROLES_VENDEDOR` | Emails → rol (fallback si no hay App Roles) |
| `SERVICES_PRODUCTOS_BASE_URL` | (solo ms-carrito) URL de ms-productos |

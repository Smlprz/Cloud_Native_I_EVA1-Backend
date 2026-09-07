# AWS API Gateway — Pedidos360

Capa perimetral: **único punto de entrada** hacia los microservicios de Productos
y Carrito. Valida el JWT de Azure AD en el borde (firma vía JWKS + `issuer` +
`audience`) y aplica CORS restringido al dominio del frontend.

## Qué contiene `openapi.yaml`

Definición del **HTTP API** lista para importar:

- `x-amazon-apigateway-authorizer` de tipo **JWT** (`issuer` + `audience` de Azure;
  el gateway descarga solo el JWKS del OIDC discovery).
- `security` global → toda ruta exige token, **salvo `GET /productos` y
  `GET /productos/{id}`** (catálogo público).
- `x-amazon-apigateway-cors` → solo el origen del frontend.
- Rutas **explícitas** (una por método) con integración `http_proxy` hacia la EC2:
  - `/productos*` → `http://18.212.91.126:8081/api/productos*`
  - `/carrito*`  → `http://18.212.91.126:8082/api/carrito*`

> Rutas explícitas en vez de `{proxy+}` / `x-amazon-apigateway-any-method`:
> esa sintaxis es de REST API (v1) y **un HTTP API la ignora al importar** (deja
> rutas sin crear → 404).

## Antes de importar: reemplazar

| Valor | Dónde se obtiene |
|---|---|
| `REEMPLAZAR_TENANT_ID` | Entra ID → Overview → Directory (tenant) ID |
| `REEMPLAZAR_CLIENT_ID` | App Registration `pedidos360-api` → Application (client) ID |
| `18.212.91.126` (x4) | IP pública actual de la EC2 (cámbiala si la instancia se reinició sin Elastic IP) |
| `allowOrigins` | agregar el dominio real del frontend cuando exista |

## Desplegar (consola)

1. API Gateway → **Create API** → **HTTP API** → **Import**.
2. Subir el `openapi.yaml` ya editado.
3. En **Routes**, verificar que aparecen las 7 rutas
   (`GET/POST /productos`, `GET/PUT/DELETE /productos/{id}`, `GET/DELETE /carrito`,
   `POST /carrito/items`, `PUT/DELETE /carrito/items/{itemId}`,
   `POST /carrito/checkout`, `GET /carrito/pedidos`).
4. En **Authorization**, verificar que el authorizer `azureJwt` está en todas
   menos en los `GET` de productos.
5. Crear un stage (ej. `prod`) con **Auto-deploy** → anotar la **Invoke URL**.

## Requisito en la EC2

El Security Group debe permitir **entrada** a los puertos **8081** y **8082**
(el gateway llega por IP pública porque `connectionType: INTERNET`).

## Evidencias para la evaluación

```bash
curl -i https://<invoke-url>/prod/productos
```
→ **200** con el catálogo (público).

```bash
curl -i https://<invoke-url>/prod/carrito
```
→ **401** (ruta protegida, sin token).

```bash
curl -i -H "Authorization: Bearer $TOKEN" https://<invoke-url>/prod/carrito
```
→ **200** con un token válido de Azure.

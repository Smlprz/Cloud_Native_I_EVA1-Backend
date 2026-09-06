# AWS API Gateway — Pedidos360

Capa perimetral: **único punto de entrada** hacia los microservicios de Productos
y Carrito. Valida el JWT de Azure AD en el borde (firma vía JWKS + `issuer` +
`audience`) y aplica CORS restringido al dominio del frontend.

## Qué contiene

- `openapi.yaml` — definición del **HTTP API** lista para importar. Incluye:
  - `x-amazon-apigateway-authorizer` de tipo **JWT** apuntando al `issuer` y
    `audience` de Azure (el gateway descarga solo el JWKS del OIDC discovery).
  - `security` global → toda ruta exige token, salvo `GET /productos` (catálogo público).
  - `x-amazon-apigateway-cors` → solo el origen del frontend.
  - Integraciones `http_proxy` hacia los microservicios (rutas limpias
    `/productos/*` y `/carrito/*`).

## Antes de importar: reemplazar placeholders

| Placeholder | Dónde se obtiene |
|---|---|
| `REEMPLAZAR_TENANT_ID` | Azure Portal → Microsoft Entra ID → Overview → Tenant ID |
| `REEMPLAZAR_CLIENT_ID` | App Registration de la API → Application (client) ID / App ID URI |
| `REEMPLAZAR-DOMINIO-FRONTEND` | Dominio donde se publica el frontend Angular/React |
| `REEMPLAZAR-HOST-INTERNO-PRODUCTOS` | DNS del ALB / IP privada de las EC2 de ms-productos |
| `REEMPLAZAR-HOST-INTERNO-CARRITO` | DNS del ALB / IP privada de las EC2 de ms-carrito |

## Desplegar (consola)

1. API Gateway → **Create API** → **HTTP API** → **Import**.
2. Pegar / subir `openapi.yaml` ya editado.
3. Revisar que el **Authorizer** `azureJwt` quedó asociado a todas las rutas
   excepto `GET /productos`.
4. Crear un stage (ej. `prod`) con **Auto-deploy**.
5. Anotar la **Invoke URL** — es la que consume el frontend.

## Desplegar (AWS CLI, resumido)

```bash
aws apigatewayv2 import-api --body file://openapi.yaml
# tomar el ApiId devuelto
aws apigatewayv2 create-stage --api-id <ApiId> --stage-name prod --auto-deploy
```

## Evidencias para la evaluación

```bash
# 1. Petición SIN token a una ruta protegida -> 401
curl -i https://<invoke-url>/carrito

# 2. Petición con token inválido/expirado -> 401
curl -i -H "Authorization: Bearer eyJ...roto..." https://<invoke-url>/carrito

# 3. Petición con token válido de Azure -> 200
curl -i -H "Authorization: Bearer $TOKEN" https://<invoke-url>/carrito

# 4. Catálogo público (sin token) -> 200
curl -i https://<invoke-url>/productos
```

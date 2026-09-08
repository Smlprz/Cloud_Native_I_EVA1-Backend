# Instrucciones para el compañero — AWS + Azure (Pedidos360)

Todo el código está listo y pusheado en
`https://github.com/Smlprz/Backend-Cloud_Native-I` (commit `ded79f5`).

Ahora hay **3 microservicios**: `ms-productos`, `ms-carrito` y `ms-auth` (identidad).
Cada uno va en su propia EC2.

## Datos ya definidos

| Recurso | Valor |
|---|---|
| Tenant Azure | `e5372bf0-c5e3-4286-887c-79069f209c1f` |
| Client ID API (`pedidos360-api`) | `ce3e5b61-0b01-4a67-a42b-f15ccb364024` |
| Client ID SPA (`pedidos360-frontend`) | `f79c81c1-7533-4ecf-9931-f334a58cc6f0` |
| API Gateway (API ID) | `gzlei5wlz3` |
| Invoke URL | `https://gzlei5wlz3.execute-api.us-east-1.amazonaws.com/prod` |
| EC2 #1 productos + MySQL | `3.225.255.55` (8081, 3307) |
| EC2 #2 carrito | `44.193.187.57` (8082) |
| EC2 #3 auth (Elastic IP, fija) | `35.170.38.245` (8083) |

---

## PARTE 1 — Azure (2 cosas)

### 1.1 Redirect URIs del frontend
App Registration **`pedidos360-frontend`** → *Authentication* → *Redirect URIs* (tipo **SPA**) → agregar:
- `http://localhost:3000`
- `http://localhost:3001`

### 1.2 Permisos de API
App Registration **`pedidos360-frontend`** → *API permissions* → verificar que estén agregados y con **Grant admin consent**:
- `api://ce3e5b61-0b01-4a67-a42b-f15ccb364024/productos.read`
- `api://ce3e5b61-0b01-4a67-a42b-f15ccb364024/carrito.read`
- `api://ce3e5b61-0b01-4a67-a42b-f15ccb364024/carrito.write`

> Si los scopes de la API tienen otro nombre (no `productos.read` / `carrito.read` / `carrito.write`), avisar a Samuel para ajustar el frontend.

---

## PARTE 2 — AWS: desplegar los 3 microservicios

Guía detallada en el repo: `infra/deploy-ec2/README.md`.

### 2.1 Preparar cada una de las 3 EC2 (por SSH)

```
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/download/v2.32.1/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
```

### 2.2 EC2 #1 — productos + MySQL (3.225.255.55) — LEVANTAR PRIMERO

```
cd ~/Backend-Cloud_Native-I/infra/deploy-ec2/ec2-productos
sudo docker compose up -d --build
```

### 2.3 EC2 #2 — carrito (44.193.187.57)

```
cd ~/Backend-Cloud_Native-I/infra/deploy-ec2/ec2-carrito
PRODUCTOS_EC2_HOST=3.225.255.55 sudo -E docker compose up -d --build
```

### 2.4 EC2 #3 — auth (35.170.38.245)

```
cd ~/Backend-Cloud_Native-I/infra/deploy-ec2/ec2-auth
PRODUCTOS_EC2_HOST=3.225.255.55 sudo -E docker compose up -d --build
```

### 2.5 Security Groups — Inbound rules

| EC2 | Puertos a abrir |
|---|---|
| #1 productos | TCP 8081, TCP 3307, TCP 22 |
| #2 carrito | TCP 8082, TCP 22 |
| #3 auth | TCP 8083, TCP 22 |

(El 3307 en la #1 es para que la #2 y la #3 se conecten a la MySQL. Origen `0.0.0.0/0` para probar, o mejor las IPs privadas de #2 y #3.)

### 2.6 Verificar cada EC2 (por SSH, dentro de la máquina)

```
sudo docker compose ps
```
- En #1: `curl http://localhost:8081/api/productos`  → JSON con 8 productos
- En #2: `curl -i http://localhost:8082/api/carrito`  → 401 (correcto, sin token)
- En #3: `curl http://localhost:8083/api/auth/health` → `{"status":"UP",...}`

---

## PARTE 3 — AWS: API Gateway

El `openapi.yaml` del repo ya tiene las 3 IPs y el issuer/audience reales.
Re-importar sobre el API existente:

```
aws apigatewayv2 reimport-api --api-id gzlei5wlz3 --body file://openapi.yaml --region us-east-1
aws apigatewayv2 create-deployment --api-id gzlei5wlz3 --stage-name prod --region us-east-1
```

(El `openapi.yaml` está en `infra/api-gateway/openapi.yaml`. Copiarlo a una ruta sin espacios si se ejecuta desde Windows.)

### Verificar el gateway (desde cualquier PC)

```
curl -i https://gzlei5wlz3.execute-api.us-east-1.amazonaws.com/prod/productos
curl -i https://gzlei5wlz3.execute-api.us-east-1.amazonaws.com/prod/carrito
curl -i https://gzlei5wlz3.execute-api.us-east-1.amazonaws.com/prod/auth/health
```

Esperado: **200** / **401** / **200**.

---

## PARTE 4 — Limpieza

- Terminar la EC2 vieja `18.212.91.126` (era el "todo en uno", ya no se usa).

---

## Resumen de tareas

- [ ] Azure: Redirect URIs `http://localhost:3000` y `:3001` en `pedidos360-frontend`
- [ ] Azure: permisos de API + admin consent (3 scopes)
- [ ] EC2 #1, #2, #3: instalar Docker + compose + swap + clone del repo
- [ ] Levantar #1 (productos + MySQL), luego #2 (carrito), luego #3 (auth)
- [ ] Security Groups: abrir puertos de cada EC2
- [ ] Verificar los 3 servicios por SSH
- [ ] Re-importar `openapi.yaml` en el API Gateway `gzlei5wlz3` + deploy
- [ ] Verificar el gateway (200 / 401 / 200)
- [ ] Terminar la EC2 vieja `18.212.91.126`

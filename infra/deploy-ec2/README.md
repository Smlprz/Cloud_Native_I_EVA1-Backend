# Despliegue: una EC2 por microservicio

El profesor pide **una instancia EC2 por microservicio**. Aquí está la topología y
los pasos. (Para desarrollo local sigue estando `infra/docker-compose.yml`, que
levanta los 3 en una sola máquina.)

```
                         AWS API Gateway (gzlei5wlz3)
                 /productos/*      /carrito/*        /auth/*
                     |                 |                |
        ┌────────────▼──────┐  ┌───────▼───────┐  ┌─────▼────────┐
        │  EC2 #1 PRODUCTOS  │  │ EC2 #2 CARRITO│  │ EC2 #3 AUTH  │
        │  ms-productos :8081│  │ ms-carrito    │  │ ms-auth :8083│
        │  + MySQL   :3307   │  │        :8082  │  │              │
        └────────┬──────────┘  └───────┬───────┘  └─────┬────────┘
                 │  productos_db        │ carrito_db      │ auth_db
                 └──────────────────────┴─────────────────┘
                          (todas en la MySQL de la EC2 #1)
```

- **EC2 #1** aloja además el contenedor **MySQL** con los 3 esquemas
  (`productos_db`, `carrito_db`, `auth_db`).
- **EC2 #2 y #3** no tienen BD propia: se conectan por red a
  `EC2#1:3307`.

## Security Groups

| EC2 | Puertos de entrada a abrir |
|---|---|
| #1 productos | `8081` (API), `3307` (MySQL, para #2 y #3), `22` (SSH) |
| #2 carrito | `8082`, `22` |
| #3 auth | `8083`, `22` |

> Ideal: que #2 y #3 accedan a #1 por su **IP privada** del VPC (más seguro y la IP
> privada no cambia). Si usan IP pública, hay que abrir `3307` a `0.0.0.0/0`.

## Pasos en cada EC2

Requisitos (una vez): Docker + plugin compose (ver `docs/GUIA-COMPANERO.md`), y un
swapfile de 2 GB si la instancia es `t3.micro`.

**EC2 #1 — productos + MySQL**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-productos
sudo docker compose up -d --build
```

**EC2 #2 — carrito**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-carrito
PRODUCTOS_EC2_HOST=<ip-privada-o-publica-de-EC2-1> sudo -E docker compose up -d --build
```

**EC2 #3 — auth**
```bash
git clone https://github.com/Smlprz/Backend-Cloud_Native-I.git
cd Backend-Cloud_Native-I/infra/deploy-ec2/ec2-auth
PRODUCTOS_EC2_HOST=<ip-privada-o-publica-de-EC2-1> sudo -E docker compose up -d --build
```

## API Gateway

En `infra/api-gateway/openapi.yaml`, poner la IP de cada EC2 en sus rutas:
- `/productos*` → `http://<EC2-1>:8081/...`
- `/carrito*`  → `http://<EC2-2>:8082/...`
- `/auth*`     → `http://<EC2-3>:8083/...`

Luego `aws apigatewayv2 reimport-api --api-id gzlei5wlz3 --body file://openapi.yaml --region us-east-1`.

## Nota sobre `auth_db`

El script `infra/mysql/init/01-init-databases.sql` crea las 3 bases, pero **solo la
primera vez** que se crea el volumen de MySQL. Si la EC2 #1 ya tenía MySQL con datos,
crear la base a mano una vez:
```bash
sudo docker exec -i pedidos360-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4; GRANT ALL PRIVILEGES ON auth_db.* TO 'pedidos'@'%'; FLUSH PRIVILEGES;"
```

-- Se ejecuta una sola vez, la primera vez que arranca el contenedor de MySQL.
-- Crea una base de datos (esquema) por microservicio y un usuario compartido de aplicacion.

CREATE DATABASE IF NOT EXISTS productos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS carrito_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'pedidos'@'%' IDENTIFIED BY 'pedidos';
GRANT ALL PRIVILEGES ON productos_db.* TO 'pedidos'@'%';
GRANT ALL PRIVILEGES ON carrito_db.*   TO 'pedidos'@'%';
GRANT ALL PRIVILEGES ON auth_db.*      TO 'pedidos'@'%';
FLUSH PRIVILEGES;

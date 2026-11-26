#!/bin/sh
# Reemplaza las IP PUBLICAS de las integraciones en openapi.yaml y openapi-frontend.yaml
# (cada servicio se reconoce por su puerto) y el origen del frontend en el CORS.
#
#   PRODUCTOS_IP=3.1.1.1 CARRITO_IP=3.1.1.2 ... FRONTEND_ORIGIN=https://3-1-1-9.sslip.io \
#   sh actualizar-ips.sh
#
# Variables (todas opcionales; la que falta no se toca):
#   PRODUCTOS_IP (8081)  CARRITO_IP (8082)  AUTH_IP (8083)       ORDEN_IP (8085)
#   RESENAS_IP (8086)    RABBIT_ADMIN_IP (8087)  PAGO_IP (8088)   ANALYTICS_IP (8089)
#   KAFKA_ADMIN_IP (8091)  CUPONES_IP (8092)
#   FRONTEND_ORIGIN: origen HTTPS del frontend, sin barra final (reemplaza al *.sslip.io actual).
#
# Despues: aws apigatewayv2 reimport-api --api-id <id> --body file://openapi-frontend.yaml
set -e
cd "$(dirname "$0")"

reemplazar() {
  ip=$1; puerto=$2; nombre=$3
  if [ -z "$ip" ]; then
    echo "Sin cambios $nombre (:$puerto)"
    return
  fi
  sed -i -E "s#http://[^/:\"' ]+:$puerto/#http://$ip:$puerto/#g" openapi.yaml openapi-frontend.yaml
  echo "OK $nombre -> $ip:$puerto"
}

reemplazar "$PRODUCTOS_IP"       8081 ms-productos
reemplazar "$CARRITO_IP"         8082 ms-carrito
reemplazar "$AUTH_IP"            8083 ms-auth
reemplazar "$ORDEN_IP"           8085 ms-orden
reemplazar "$RESENAS_IP"         8086 ms-resenas
reemplazar "$RABBIT_ADMIN_IP"    8087 ms-rabbit-admin
reemplazar "$PAGO_IP"            8088 ms-pago
reemplazar "$ANALYTICS_IP"       8089 ms-analytics-recommendations
reemplazar "$KAFKA_ADMIN_IP"     8091 ms-kafka-admin
reemplazar "$CUPONES_IP"         8092 ms-cupones

if [ -n "$FRONTEND_ORIGIN" ]; then
  sed -i -E "s#^(  - )https://[^ ]+\.sslip\.io\$#\1$FRONTEND_ORIGIN#" openapi-frontend.yaml
  echo "OK CORS allowOrigins -> $FRONTEND_ORIGIN"
fi

echo
echo "Integraciones que quedaron:"
grep -ho "http://[^/]*:80[0-9][0-9]" openapi.yaml openapi-frontend.yaml | sort | uniq -c

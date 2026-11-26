#!/bin/sh
# Genera targets/servicios.json y targets/rabbitmq.json para Prometheus.
#
#   PRODUCTOS_HOST=10.0.1.10 CARRITO_HOST=10.0.1.11 ... sh generar-targets.sh
#
# Usar las IP PRIVADAS de cada EC2. Un servicio sin variable se omite (se avisa).
# Variables: PRODUCTOS_HOST CARRITO_HOST AUTH_HOST NOTIFICACIONES_HOST ORDEN_HOST RESENAS_HOST
#            RABBIT_ADMIN_HOST PAGO_HOST ANALYTICS_HOST KAFKA_ADMIN_HOST CUPONES_HOST RABBITMQ_HOST
set -e
cd "$(dirname "$0")"
mkdir -p targets

salida=targets/servicios.json
primero=1
printf '[\n' > "$salida"

agregar() {
  nombre=$1; host=$2; puerto=$3
  if [ -z "$host" ]; then
    echo "Omitido $nombre (falta la variable con su IP)"
    return
  fi
  [ "$primero" -eq 1 ] || printf ',\n' >> "$salida"
  primero=0
  printf '  {"targets": ["%s:%s"], "labels": {"servicio": "%s"}}' "$host" "$puerto" "$nombre" >> "$salida"
  echo "OK $nombre -> $host:$puerto"
}

agregar ms-productos                 "$PRODUCTOS_HOST"      8081
agregar ms-carrito                   "$CARRITO_HOST"        8082
agregar ms-auth                      "$AUTH_HOST"           8083
agregar ms-notificaciones            "$NOTIFICACIONES_HOST" 8084
agregar ms-orden                     "$ORDEN_HOST"          8085
agregar ms-resenas                   "$RESENAS_HOST"        8086
agregar ms-rabbit-admin              "$RABBIT_ADMIN_HOST"   8087
agregar ms-pago                      "$PAGO_HOST"           8088
agregar ms-analytics-recommendations "$ANALYTICS_HOST"      8089
agregar ms-kafka-admin               "$KAFKA_ADMIN_HOST"    8091
agregar ms-cupones                   "$CUPONES_HOST"        8092
printf '\n]\n' >> "$salida"

# Cluster RabbitMQ: nodo 1 en 15692 y nodo 2 en 15693 de la misma EC2.
if [ -n "$RABBITMQ_HOST" ]; then
  printf '[{"targets": ["%s:15692", "%s:15693"], "labels": {"servicio": "rabbitmq"}}]\n' "$RABBITMQ_HOST" "$RABBITMQ_HOST" > targets/rabbitmq.json
  echo "OK rabbitmq -> $RABBITMQ_HOST:15692 y :15693"
else
  printf '[]\n' > targets/rabbitmq.json
  echo "Omitido rabbitmq (falta RABBITMQ_HOST)"
fi

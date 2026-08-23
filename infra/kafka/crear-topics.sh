#!/usr/bin/env bash
# Crea los topics del sistema y sus colas de mensajes muertos.
#
# Los topics se crean acá y no de forma automática al primer uso: un topic creado por
# accidente, con las particiones por defecto y el nombre mal escrito, es una de las
# formas más caras de perder mensajes en producción. Por eso el broker tiene la creación
# automática apagada.
set -euo pipefail

BROKER="${BROKER:-kafka:9092}"
PARTICIONES="${PARTICIONES:-3}"
REPLICAS="${REPLICAS:-1}"

TOPICS=(
  "mediturno.reservas.eventos"
  "mediturno.reservas.eventos.dlq"
  "mediturno.notificaciones.solicitudes"
  "mediturno.notificaciones.solicitudes.dlq"
  "mediturno.pagos.confirmaciones"
  "mediturno.pagos.confirmaciones.dlq"
)

echo "Creando topics en ${BROKER}..."
for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BROKER}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTICIONES}" \
    --replication-factor "${REPLICAS}"
  echo "  ok  ${topic}"
done

echo
echo "Topics existentes:"
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BROKER}" --list

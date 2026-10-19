#!/usr/bin/env bash
# Verificación del ambiente del Release 2.1.
#
# Revisa una por una las piezas que tienen que estar en pie y deja una lista de
# resultados. No prueba ninguna regla de negocio: prueba que las cañerías estén
# conectadas, para que cuando algo falle se pueda distinguir "el ambiente está mal" de
# "el sistema está mal".
#
#   bash verificar-ambiente.sh
#
# Devuelve 0 si todo está bien y 1 si algo falla.

set -uo pipefail

OK=0
FALLAS=0
OMITIDAS=0

verde()  { printf '  \033[32m✔\033[0m  %s\n' "$1"; OK=$((OK + 1)); }
rojo()   { printf '  \033[31m✘\033[0m  %s\n'   "$1"; FALLAS=$((FALLAS + 1)); }
gris()   { printf '  \033[90m·\033[0m  %s\n'   "$1"; OMITIDAS=$((OMITIDAS + 1)); }
titulo() { printf '\n\033[1m%s\033[0m\n' "$1"; }

comprobar() {           # comprobar "descripción" comando...
  local descripcion="$1"; shift
  if "$@" >/dev/null 2>&1; then verde "$descripcion"; else rojo "$descripcion"; fi
}

# El comando de compose cambió de nombre entre versiones.
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  COMPOSE=()
fi

printf '\033[1mMediTurno 2.0 — Release 2.1 · verificación del ambiente\033[0m\n'

# ------------------------------------------------------------------ 1. herramientas
titulo "1. Herramientas"

comprobar "Docker está instalado"        command -v docker
comprobar "El demonio de Docker responde" docker info
if [ ${#COMPOSE[@]} -gt 0 ]; then
  verde "Docker Compose está disponible"
else
  rojo "Docker Compose está disponible"
fi
comprobar "Java está instalado"          command -v java
comprobar "Maven está instalado"         command -v mvn

if command -v java >/dev/null 2>&1; then
  VERSION_JAVA=$(java -version 2>&1 | grep -oE 'version "[0-9]+' | head -1 | grep -oE '[0-9]+')
  if [ "${VERSION_JAVA:-0}" -ge 17 ] 2>/dev/null; then
    verde "Java $VERSION_JAVA cumple el mínimo de 17"
  else
    rojo "Java $VERSION_JAVA no alcanza el mínimo de 17"
  fi
fi

# ------------------------------------------------------------ 2. contenedores arriba
titulo "2. Contenedores"

CONTENEDORES=(mediturno-postgres mediturno-redis mediturno-kafka
              mediturno-validador-prevision mediturno-proveedor-mensajeria
              mediturno-ms-tarificacion mediturno-ms-pacientes mediturno-ms-reservas
              mediturno-ms-notificaciones mediturno-ms-pagos)

for c in "${CONTENEDORES[@]}"; do
  estado=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null | tr -d '[:space:]')
  estado=${estado:-ausente}
  salud=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-chequeo{{end}}' "$c" 2>/dev/null | tr -d '[:space:]')
  case "$estado" in
    running)
      if [ "$salud" = "healthy" ] || [ "$salud" = "sin-chequeo" ]; then
        verde "$c   ($estado${salud:+, $salud})"
      else
        rojo "$c   ($estado, $salud)"
      fi
      ;;
    ausente) rojo "$c   no existe — ¿corrió 'docker compose up -d --build'?" ;;
    *)       rojo "$c   ($estado)" ;;
  esac
done

# ------------------------------------------------------------------ 3. PostgreSQL
titulo "3. PostgreSQL"

comprobar "acepta conexiones" \
  docker exec mediturno-postgres pg_isready -U mediturno -d mediturno

for base in mediturno_reservas mediturno_pacientes; do
  if docker exec mediturno-postgres psql -U mediturno -d "$base" -c 'select 1' >/dev/null 2>&1; then
    verde "la base $base existe y responde"
  else
    rojo "la base $base existe y responde"
  fi
done

# ---------------------------------------------------------------------- 4. Redis
titulo "4. Redis"

if [ "$(docker exec mediturno-redis redis-cli ping 2>/dev/null | tr -d '\r')" = "PONG" ]; then
  verde "responde PING"
else
  rojo "responde PING"
fi

if docker exec mediturno-redis redis-cli set mediturno:verificacion ok EX 30 >/dev/null 2>&1 \
   && [ "$(docker exec mediturno-redis redis-cli get mediturno:verificacion 2>/dev/null | tr -d '\r')" = "ok" ]; then
  verde "escribe y lee una clave con expiración"
  docker exec mediturno-redis redis-cli del mediturno:verificacion >/dev/null 2>&1
else
  rojo "escribe y lee una clave con expiración"
fi

# ---------------------------------------------------------------------- 5. Kafka
titulo "5. Kafka"

TOPICS_ESPERADOS=(
  mediturno.reservas.eventos
  mediturno.reservas.eventos.dlq
  mediturno.notificaciones.solicitudes
  mediturno.notificaciones.solicitudes.dlq
  mediturno.pagos.confirmaciones
  mediturno.pagos.confirmaciones.dlq
)

TOPICS_ACTUALES=$(docker exec mediturno-kafka \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null || echo "")

if [ -z "$TOPICS_ACTUALES" ]; then
  rojo "el broker responde"
  for t in "${TOPICS_ESPERADOS[@]}"; do gris "topic $t   (no se pudo consultar)"; done
else
  verde "el broker responde"
  for t in "${TOPICS_ESPERADOS[@]}"; do
    if grep -qx "$t" <<<"$TOPICS_ACTUALES"; then
      verde "topic $t"
    else
      rojo "topic $t   no existe"
    fi
  done
fi

# ------------------------------------------------------------------ 6. servicios
titulo "6. Servicios"

declare -A SERVICIOS=(
  [ms-tarificacion]=8081
  [ms-reservas]=8082
  [ms-pacientes]=8083
  [ms-notificaciones]=8084
  [ms-pagos]=8085
)

for s in ms-tarificacion ms-pacientes ms-reservas ms-notificaciones ms-pagos; do
  puerto=${SERVICIOS[$s]}
  cuerpo=$(curl -sf --max-time 5 "http://localhost:${puerto}/actuator/health" 2>/dev/null || echo "")
  if grep -q '"status":"UP"' <<<"$cuerpo"; then
    verde "$s   :$puerto   UP"
  elif [ -n "$cuerpo" ]; then
    rojo "$s   :$puerto   responde pero no está UP"
  else
    rojo "$s   :$puerto   no responde"
  fi
done

# ------------------------------------------------------- 7. servicios externos simulados
titulo "7. Dobles de los servicios externos"

if curl -sf --max-time 5 "http://localhost:8090/prevision/5.555.555-5" >/dev/null 2>&1; then
  verde "Validador de Previsión responde en :8090"
else
  rojo "Validador de Previsión responde en :8090"
fi

if curl -sf --max-time 5 -o /dev/null -X POST "http://localhost:8091/mensajes" \
     -H 'Content-Type: application/json' \
     -d '{"canal":"SMS","destinatario":"5.555.555-5","cuerpo":"verificación"}' 2>/dev/null; then
  verde "Proveedor de mensajería responde en :8091"
else
  rojo "Proveedor de mensajería responde en :8091"
fi

# ------------------------------------------------------------ 8. cañerías conectadas
titulo "8. Las piezas conectadas entre sí"

# ms-pacientes devuelve un paciente del padrón de ejemplo.
if curl -sf --max-time 5 "http://localhost:8083/api/pacientes/5.555.555-5" 2>/dev/null \
     | grep -q '"rut"'; then
  verde "ms-pacientes devuelve un paciente del padrón"
else
  rojo "ms-pacientes devuelve un paciente del padrón"
fi

# ms-tarificacion calcula. No se revisa el monto: solo que conteste con el desglose.
if curl -sf --max-time 5 -X POST "http://localhost:8081/api/tarificacion/calcular" \
     -H 'Content-Type: application/json' \
     -d '{"paciente":{"rut":"5.555.555-5","fechaNacimiento":"1986-09-17","prevision":"FONASA","tramoFonasa":"B"},"especialidad":"MEDICINA_GENERAL","fechaHoraAtencion":"2026-10-05T09:00:00"}' \
     2>/dev/null | grep -q '"copago"'; then
  verde "ms-tarificacion calcula y devuelve el desglose"
else
  rojo "ms-tarificacion calcula y devuelve el desglose"
fi

# ms-reservas consulta el padrón por HTTP: si contesta esto, la cañería HTTP está viva.
if curl -sf --max-time 5 "http://localhost:8082/api/pacientes" 2>/dev/null | grep -q '"rut"'; then
  verde "ms-reservas alcanza a ms-pacientes por HTTP"
else
  rojo "ms-reservas alcanza a ms-pacientes por HTTP"
fi

# La interfaz web responde.
if curl -sf --max-time 5 -o /dev/null "http://localhost:8082/reservas" 2>/dev/null; then
  verde "la interfaz web de ms-reservas responde"
else
  rojo "la interfaz web de ms-reservas responde"
fi

# El webhook de pagos acepta una notificación y la publica.
ORDEN="MT-000000"
if curl -sf --max-time 5 -X POST "http://localhost:8085/api/webhooks/webpay" \
     -H 'Content-Type: application/json' \
     -d "{\"buy_order\":\"${ORDEN}\",\"session_id\":\"verificacion\",\"amount\":1,\"status\":\"FAILED\",\"authorization_code\":\"verificacion-$$\",\"transaction_date\":\"2026-10-05T09:00:00\"}" \
     2>/dev/null | grep -q 'RECIBIDO'; then
  verde "ms-pagos recibe el webhook de Webpay y lo publica"
else
  rojo "ms-pagos recibe el webhook de Webpay y lo publica"
fi

# Los circuitos de Resilience4j están registrados y cerrados.
CIRCUITOS=$(curl -sf --max-time 5 "http://localhost:8082/actuator/circuitbreakers" 2>/dev/null || echo "")
for c in tarificacion pacientes validadorPrevision; do
  if grep -q "\"$c\"" <<<"$CIRCUITOS"; then
    verde "circuito '$c' registrado"
  else
    rojo "circuito '$c' registrado"
  fi
done

# -------------------------------------------------------------------- resumen
TOTAL=$((OK + FALLAS))
printf '\n\033[1mResumen\033[0m\n'
printf '  %s de %s comprobaciones en verde' "$OK" "$TOTAL"
[ "$OMITIDAS" -gt 0 ] && printf ', %s omitidas' "$OMITIDAS"
printf '\n\n'

if [ "$FALLAS" -eq 0 ]; then
  printf '\033[32mEl ambiente está completo. Puede empezar.\033[0m\n'
  exit 0
fi

printf '\033[31m%s comprobación(es) en rojo.\033[0m\n' "$FALLAS"
cat <<'AYUDA'

Qué mirar primero:

  · Si faltan contenedores        docker compose up -d --build
  · Si alguno quedó unhealthy     docker compose logs <servicio>
  · Si faltan los topics          docker compose up kafka-topics
  · Si nada responde en los       espere: la primera vez los servicios demoran
    puertos y los contenedores    hasta un minuto en quedar healthy
    recién partieron
  · Para partir de cero           docker compose down -v && docker compose up -d --build

El Anexo C de la Guía del Estudiante tiene la tabla de síntoma, causa y solución.
AYUDA
exit 1

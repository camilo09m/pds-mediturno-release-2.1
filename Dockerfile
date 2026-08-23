# Imagen única para los cinco servicios. Cuál se empaqueta lo decide el argumento
# SERVICIO, de modo que la etapa de compilación se construye una sola vez y las cinco
# imágenes la reutilizan desde la caché.
#
#   docker build --build-arg SERVICIO=ms-reservas -t mediturno/ms-reservas:2.1.0 .

FROM maven:3.9.9-eclipse-temurin-17 AS compilacion
WORKDIR /construccion

# Primero solo los pom: si no cambian, la descarga de dependencias se toma de la caché.
COPY pom.xml ./
COPY ms-tarificacion/pom.xml    ms-tarificacion/
COPY ms-pacientes/pom.xml       ms-pacientes/
COPY ms-reservas/pom.xml        ms-reservas/
COPY ms-notificaciones/pom.xml  ms-notificaciones/
COPY ms-pagos/pom.xml           ms-pagos/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre AS ejecucion
ARG SERVICIO
ENV SERVICIO=${SERVICIO}
WORKDIR /app

# curl es para el healthcheck del contenedor: sin él, docker compose nunca sabría
# si el servicio está contestando o solo encendido.
RUN apt-get update \
 && apt-get install --no-install-recommends -y curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --create-home --shell /usr/sbin/nologin mediturno
COPY --from=compilacion /construccion/${SERVICIO}/target/${SERVICIO}-2.1.0.jar /app/app.jar
COPY datos /app/datos
RUN chown -R mediturno:mediturno /app
USER mediturno

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

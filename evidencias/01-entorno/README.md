# Entorno y línea base (E3-01)

## Entorno

| Ítem | Valor |
|---|---|
| Commit base | c59e9f4 |
| Sistema operativo | Fedora Linux 42, kernel 6.19.14 |
| Procesador | Intel Core i7-12700H |
| RAM | 15 GiB |
| Java | OpenJDK 21.0.11 |
| Maven | 3.9.9 |
| Docker | 29.5.3 |
| Docker Compose | v5.1.4 |

## Línea base (commit c59e9f4, sin modificaciones)

Comando: `mvn -B clean verify`. Resultado: BUILD SUCCESS en 228 s.
Log: `linea-base-mvn-verify.log`.

| Clase | Módulo | Pruebas | Fallas | Tiempo |
|---|---|---|---|---|
| TarificacionRespondeIT | ms-tarificacion | 2 | 0 | 12,4 s |
| PadronRespondeIT | ms-pacientes | 3 | 0 | 26,0 s |
| ContextoLevantaIT | ms-reservas | 2 | 0 | 90,6 s |
| KafkaRespondeIT | ms-reservas | 2 | 0 | 1,0 s |
| PostgresRespondeIT | ms-reservas | 2 | 0 | 0,5 s |
| RedisRespondeIT | ms-reservas | 3 | 0 | 0,8 s |
| Total | | 14 | 0 | |

ms-notificaciones y ms-pagos no tienen pruebas en el repositorio base.

## Hallazgo: Testcontainers 1.20.2 incompatible con Docker 29

Con la versión del repositorio base, Testcontainers no logra conectarse a Docker:
`client version 1.32 is too old. Minimum supported API version is 1.40`.
El mismo error hace fallar el workflow "Verificación del repositorio base" en GitHub Actions.

Corrección: `testcontainers.version` 1.20.2 → 1.21.4 en `pom.xml` (1.21.3 sigue fallando).
Con 1.21.4: BUILD SUCCESS en 132 s, 14 pruebas, 0 fallas.
Log: `mvn-verify-testcontainers-1.21.4.log`.

## Ambiente completo

`docker compose up -d` y `bash verificar-ambiente.sh`: 43 de 43 comprobaciones en verde.
Salidas: `verificar-ambiente.txt` y `docker-compose-ps.txt`.

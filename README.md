# MediTurno 2.0 — Release 2.1

Repositorio base de la **Entrega 3 · Integración y Regresión**.

MediTurno es el sistema de agendamiento de horas médicas del Centro Médico Los Andes. Hasta
la versión 2.0 eran dos servicios sueltos. El Release 2.1 los reúne en un monorepo, agrega
tres servicios más y cambia la forma en que se comunican: lo que antes era una llamada a un
método ahora es un salto de red o un mensaje en una cola.

Ese cambio es el objeto de la entrega. Cada salto nuevo es una oportunidad de falla que en
la versión 2.0 no existía.

---

## 1. Qué hay adentro

| Módulo | Puerto | Qué hace | Guarda datos en |
|---|---|---|---|
| `ms-tarificacion` | 8081 | Calcula arancel y copago (RN-TAR-01 a RN-TAR-10) | — |
| `ms-reservas` | 8082 | Ciclo de vida de la reserva (RN-RES-01 a RN-RES-11) | PostgreSQL + Redis |
| `ms-pacientes` | 8083 | Padrón de pacientes | PostgreSQL |
| `ms-notificaciones` | 8084 | Despacha avisos al paciente | Redis |
| `ms-pagos` | 8085 | Webhook de Webpay | Redis |

Y dos terceros, que no son parte de MediTurno y vienen simulados con WireMock:

| Doble | Puerto | Contrato relevante |
|---|---|---|
| Validador de Previsión | 8090 | `GET /prevision/{rut}` · SLA 500 ms · timeout 3 s · 200 / 404 / 429 / 503 |
| Proveedor de mensajería | 8091 | `POST /mensajes` · cuota de 500 envíos por hora, luego 429 |

---

## 2. Cómo se levanta

### 2.1 Con contenedores — el ambiente del Release 2.1

Requiere Docker con Compose v2. Es el ambiente real: cinco servicios, PostgreSQL, Redis,
Kafka y los dos dobles externos.

```bash
docker compose up -d --build      # la primera vez se demora: compila los cinco módulos
docker compose ps                 # todos deben quedar en healthy
docker compose logs -f ms-reservas
docker compose down -v            # baja todo y borra los datos
```

Cuando termine de levantar:

- interfaz web  → <http://localhost:8082/reservas>
- API           → <http://localhost:8082/swagger>
- padrón        → <http://localhost:8083/swagger>
- pagos         → <http://localhost:8085/swagger>

### 2.2 Sin contenedores — el perfil `efimero`

Un solo servicio, base en memoria, sin broker y sin servicios externos. Arranca en segundos
y no necesita Docker.

```bash
mvn -B -pl ms-reservas spring-boot:run -Dspring-boot.run.profiles=efimero
```

**Este es el perfil en el que siguen corriendo, sin cambios, las suites de las entregas 1 y
2.** Si una prueba suya deja de pasar en `efimero`, no es por la infraestructura nueva.

### 2.3 Verificar que el ambiente quedó bien

```bash
bash verificar-ambiente.sh
```

Revisa una por una las piezas —herramientas, contenedores, PostgreSQL, Redis, los seis
topics, los cinco servicios, los dos dobles externos y las conexiones entre ellos— y deja
una lista de resultados. Devuelve 0 si todo está en verde.

No prueba ninguna regla de negocio. Sirve para lo otro: cuando algo falle, poder distinguir
*"el ambiente está mal"* de *"el sistema está mal"*, que es una distinción que sale cara
descubrir tarde.

Lo mismo, pero desde Java y con contenedores efímeros, está en las pruebas de plomería:

```bash
mvn -B -pl ms-reservas verify -Dit.test='*RespondeIT,ContextoLevantaIT'
```

### 2.4 Compilar y probar

```bash
mvn -B clean verify              # todo: compila, pruebas rápidas y de integración
mvn -B -Prapido clean verify     # solo las rápidas — es la capa de smoke del pipeline
mvn -B -pl ms-reservas test      # un módulo
```

Las pruebas de integración (`*IT`) necesitan un demonio de Docker disponible, porque levantan
sus propias dependencias con Testcontainers. No usan el `docker compose` de arriba: cada
prueba levanta lo suyo y lo bota al terminar. **No hay ambiente compartido y no debe haberlo.**

---

## 3. Los topics

| Topic | Publica | Consume |
|---|---|---|
| `mediturno.reservas.eventos` | ms-reservas | ms-notificaciones |
| `mediturno.notificaciones.solicitudes` | ms-reservas | ms-notificaciones |
| `mediturno.pagos.confirmaciones` | ms-pagos | ms-reservas |

Cada uno tiene su cola de mensajes muertos con el sufijo `.dlq`. Un mensaje llega ahí cuando
el consumidor agota los reintentos, o cuando la carga útil no se puede interpretar.

Tres cosas que conviene saber antes de escribir la primera prueba asíncrona:

- **La clave de partición de los eventos de reserva es el id de la reserva.** Los eventos de
  una misma reserva llegan en orden. Entre reservas distintas no hay ninguna garantía.
- **La entrega es al menos una vez.** Recibir dos veces el mismo mensaje es lo normal, no la
  excepción. Todos los consumidores deduplican contra Redis por el identificador del evento.
- **Las cargas útiles viajan como JSON plano, sin cabecera de tipo.** Cada servicio declara
  su propia versión del mensaje con los campos que usa. Agregar un campo no rompe a nadie;
  quitar o renombrar uno, sí.

---

## 4. Los puntos de integración

Nueve saltos, cada uno con su modo de falla. El mapa completo, con su estrategia de prueba,
es el entregable 1 de la Parte A.

| Desde | Hacia | Cómo | Qué pasa si falla |
|---|---|---|---|
| ms-reservas | ms-tarificacion | HTTP sincrónico | La reserva se rechaza con `TAR-NO-DISPONIBLE` (503) |
| ms-reservas | ms-pacientes | HTTP sincrónico | La reserva se rechaza con `PAC-NO-DISPONIBLE` (503) |
| ms-reservas | Validador de Previsión | HTTP sincrónico, externo | Se degrada: la reserva sigue sin verificar |
| ms-reservas | PostgreSQL | JDBC | Cerrojo pesimista sobre el bloque, con espera máxima de 3 s |
| ms-reservas | Redis | TCP | Ventana de confirmación y deduplicación |
| ms-reservas | Kafka | Asíncrono | El evento se publica recién al confirmar la transacción |
| ms-pagos | Kafka | Asíncrono | Deduplica el webhook por orden y código de autorización |
| ms-notificaciones | Kafka | Asíncrono | Reintenta y deriva a la DLQ |
| ms-notificaciones | Proveedor de mensajería | HTTP sincrónico, externo | Cuota agotada va directo a la DLQ, sin reintentar |

Los tres clientes HTTP de ms-reservas van con Resilience4j: timeout, reintento con espera
creciente y circuit breaker. La configuración está en
`ms-reservas/src/main/resources/application.yml`, bajo `resilience4j`. El estado de los
circuitos se consulta en <http://localhost:8082/actuator/circuitbreakers>.

---

## 5. Qué cambió respecto de la versión 2.0

Notas de este release, tal como las publicaría el equipo de desarrollo. **El análisis de
impacto es trabajo suyo**: acá está el qué, no el a-qué-le-pega.

**Cambios funcionales**

1. **Sobrecupo.** Cada bloque admite hasta 2 pacientes, antes 1.
2. **Lista de espera con ventana de confirmación.** Al liberarse un cupo ya no se promueve a
   nadie de forma automática: se le *ofrece* al primero de la fila, que tiene 15 minutos para
   confirmar. Si no confirma, la oferta vence y pasa al siguiente. Quien deja vencer su
   ventana va al final de la fila.
3. **Multa por anulación.** RN-RES-06 baja de 30 % a 25 %.
4. **Verificación previsional.** La previsión declarada se contrasta con un servicio externo.

**Cambios de arquitectura**

5. El padrón de pacientes sale de ms-reservas y pasa a `ms-pacientes`, que se consulta por HTTP.
6. La tarificación deja de calcularse en memoria y pasa a `ms-tarificacion`, por HTTP.
7. La persistencia pasa de H2 a PostgreSQL, con cerrojo pesimista sobre el bloque.
8. Aparecen los avisos al paciente y los pagos, por mensajería asíncrona.

**Compatibilidad hacia atrás**

El Release 2.1 conserva a propósito dos puntos de compatibilidad, para que las suites escritas
contra la versión 2.0 sigan compilando sin tocarlas:

- `ReservaService` mantiene el constructor de cuatro argumentos, sin notificador.
- `RepositorioReservas.enBloqueParaActualizar` trae implementación por defecto, de modo que
  un doble de prueba que solo implemente los métodos de la 2.0 sigue siendo válido.

Si alguna de sus pruebas de las entregas 1 o 2 **no compila** contra este repositorio, es un
hallazgo: repórtelo. No debería pasar.

---

## 6. Códigos de error de la API

El código es estable y forma parte del contrato. El mensaje es informativo y puede cambiar.

| Código | HTTP | Cuándo |
|---|---|---|
| `RES-01-*`, `RES-02-*` | 400 | Bloque, día, horario o anticipación inválidos |
| `RES-03-TOPE`, `RES-04-DUPLICADA`, `RES-08-BLOQUEO` | 409 | Tope de activas, duplicada en el día, bloqueo por inasistencias |
| `RES-09-*`, `RES-10-TRANSICION`, `RES-CUPO` | 409 | Modificación no admitida, transición inválida, sin cupo |
| `RES-11-SIN-OFERTA`, `RES-11-VENCIDA` | 409 | Confirmación de lista de espera sin oferta o fuera de ventana |
| `RES-404`, `PAC-404` | 404 | No existe la reserva o el paciente |
| `PREV-NO-AFILIADO` | 422 | El Validador de Previsión no reconoce la afiliación |
| `TAR-NO-DISPONIBLE`, `PAC-NO-DISPONIBLE` | 503 | Un servicio del que dependemos no respondió |
| `VAL-*` | 400 | Error de forma en la petición |

---

## 7. Facilidades de prueba que trae el repositorio

- **Cabecera `X-Reloj-Simulado`.** Fija la hora del servicio para una petición, en formato
  ISO-8601. Sin ella, las reglas que dependen del tiempo solo se pueden ejercitar esperando.
  El valor es por petición y por hilo: la suite puede correr en paralelo.
- **`data-testid`** en los elementos de la interfaz que importan, para que los selectores de
  Playwright no dependan del texto ni de la maqueta.
- **Tarea de vencimiento apagable.** `mediturno.vencimiento-ofertas.habilitado=false` apaga el
  barrido automático. Actívelo en `false` en las pruebas de integración y llame a
  `vencerOfertas()` a mano: una tarea que se dispara sola cada minuto mientras corre la suite
  es una de las causas habituales de pruebas inestables.
- **RUT con comportamiento fijo en el Validador de Previsión**, para los casos de borde de la
  Parte A:

  | RUT | Respuesta |
  |---|---|
  | cualquier otro | 200 afiliado, 120 ms |
  | `99.999.999-9` | 404 no afiliado |
  | `98.888.888-8` | 429 cuota agotada |
  | `97.777.777-7` | 503 servicio caído |
  | `96.666.666-6` | 200 pero con 3 100 ms de latencia — sobre el timeout |

---

## 8. Integración continua

El repositorio trae dos flujos de GitHub Actions:

| Archivo | De quién es | Qué hace |
|---|---|---|
| `.github/workflows/verificacion.yml` | del docente | Compila, corre todas las pruebas y levanta el ambiente completo para verificarse a sí mismo |
| `.github/workflows/pipeline-capa-1-smoke.yml` | ejemplo | La primera de las cuatro capas del pipeline que pide la Parte B |

**La capa 1 viene hecha; las capas 2, 3 y 4 son entregable del equipo.** El archivo de
ejemplo está comentado con lo que hay que resolver en cada capa.

## 9. Qué no se modifica

El repositorio base es el objeto de prueba. Cambiarlo para que una prueba pase invierte el
ejercicio.

**No se modifica:**

- las reglas de negocio en `dominio/` y `aplicacion/`;
- los códigos de error y los códigos de estado HTTP;
- los nombres de los topics ni la forma de los mensajes;
- las rutas ni los contratos de la API;
- los `data-testid` de la interfaz.

**Sí se agrega, y es lo que se evalúa:** todo lo que vive en `src/test/`, los pactos, los
stubs de WireMock, la configuración del pipeline y la documentación de la entrega.

Si encuentra un defecto en el código base —y va a encontrar varios— **se reporta, no se
arregla**. Un defecto documentado con su prueba que lo evidencia vale; el mismo defecto
corregido en silencio no se puede evaluar.

---

## 10. Estructura

```
mediturno-release-2.1/
├── pom.xml                     agregador de los cinco módulos
├── docker-compose.yml          ambiente completo
├── Dockerfile                  imagen común; el módulo lo elige el argumento SERVICIO
├── verificar-ambiente.sh       lista de comprobación del ambiente completo
├── datos/convenios.csv         padrón de convenios de empresa (RN-TAR-08)
├── .github/workflows/          verificación del repositorio y capa 1 del pipeline
├── infra/
│   ├── postgres/init.sql       una base por servicio
│   ├── kafka/crear-topics.sh   los topics se crean acá, no por accidente
│   └── wiremock/               dobles de los dos servicios externos
├── ms-tarificacion/
├── ms-pacientes/
├── ms-reservas/
├── ms-notificaciones/
└── ms-pagos/
```

---

## 11. Antes de empezar

Verifique su ambiente siguiendo el **Anexo C de la Guía del Estudiante**, y deje la evidencia
por integrante en el Checkpoint 1. Media entrega se pierde en problemas de instalación que se
detectan en veinte minutos la primera semana.

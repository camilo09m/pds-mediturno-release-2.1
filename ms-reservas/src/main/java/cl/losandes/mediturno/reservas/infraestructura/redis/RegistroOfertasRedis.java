package cl.losandes.mediturno.reservas.infraestructura.redis;

import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista de lectura de las ventanas de confirmación abiertas, sobre Redis.
 *
 * <p>Cada oferta se guarda con expiración natural: cuando la ventana de 15 minutos
 * termina, la clave desaparece sola. Eso hace innecesario un barrido, pero introduce una
 * segunda copia de un dato que también vive en PostgreSQL. Las dos copias se escriben en
 * momentos distintos y por caminos distintos: si una falla, divergen.</p>
 */
public class RegistroOfertasRedis implements RegistroOfertas {

    private static final String PREFIJO = "mediturno:oferta:";

    private final StringRedisTemplate redis;

    public RegistroOfertasRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void abrir(Long idReserva, LocalDateTime venceEn, LocalDateTime ahora) {
        if (idReserva == null || venceEn == null) {
            return;
        }
        Duration vigencia = Duration.between(ahora, venceEn);
        if (vigencia.isZero() || vigencia.isNegative()) {
            return;
        }
        redis.opsForValue().set(PREFIJO + idReserva, venceEn.toString(), vigencia);
    }

    @Override
    public boolean vigente(Long idReserva) {
        return idReserva != null && Boolean.TRUE.equals(redis.hasKey(PREFIJO + idReserva));
    }

    @Override
    public void cerrar(Long idReserva) {
        if (idReserva != null) {
            redis.delete(PREFIJO + idReserva);
        }
    }

    /**
     * Recorre el espacio de claves con KEYS. Es una operación de orden lineal, aceptable
     * aquí porque el número de ofertas vivas está acotado por los cupos de la agenda.
     */
    @Override
    public Set<Long> vigentes() {
        Set<String> claves = redis.keys(PREFIJO + "*");
        if (claves == null) {
            return Set.of();
        }
        return claves.stream()
                .map(k -> k.substring(PREFIJO.length()))
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}

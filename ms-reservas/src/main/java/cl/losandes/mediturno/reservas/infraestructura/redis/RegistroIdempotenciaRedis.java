package cl.losandes.mediturno.reservas.infraestructura.redis;

import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Marca de mensajes procesados, sobre Redis.
 *
 * <p>Se apoya en SET NX EX, que es atómico: dos instancias del consumidor que reciban el
 * mismo mensaje a la vez no pueden obtener ambas el permiso para procesarlo.</p>
 */
public class RegistroIdempotenciaRedis implements RegistroIdempotencia {

    private static final String PREFIJO = "mediturno:procesado:";

    private final StringRedisTemplate redis;

    public RegistroIdempotenciaRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean registrarSiEsNuevo(String clave, Duration vigencia) {
        Boolean primero = redis.opsForValue().setIfAbsent(PREFIJO + clave, "1", vigencia);
        return Boolean.TRUE.equals(primero);
    }

    @Override
    public void olvidar(String clave) {
        redis.delete(PREFIJO + clave);
    }
}

package cl.losandes.mediturno.notificaciones.mensajeria;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Marca de mensajes ya procesados.
 *
 * <p>Un aviso duplicado no es inocuo: el paciente recibe dos SMS y el proveedor cobra
 * dos veces. SET NX EX es atómico, de modo que dos instancias del consumidor que reciban
 * el mismo mensaje no pueden ambas obtener el permiso para enviarlo.</p>
 */
@Component
public class RegistroIdempotencia {

    private static final String PREFIJO = "mediturno:notificado:";

    private final StringRedisTemplate redis;

    public RegistroIdempotencia(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean registrarSiEsNuevo(String clave, Duration vigencia) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(PREFIJO + clave, "1", vigencia));
    }

    /** Se llama cuando el envío falló: la clave se libera para que el reintento pase. */
    public void olvidar(String clave) {
        redis.delete(PREFIJO + clave);
    }
}

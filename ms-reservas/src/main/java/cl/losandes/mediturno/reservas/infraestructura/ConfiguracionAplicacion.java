package cl.losandes.mediturno.reservas.infraestructura;

import cl.losandes.mediturno.reservas.aplicacion.PublicadorEventos;
import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;
import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;
import cl.losandes.mediturno.reservas.aplicacion.Reloj;
import cl.losandes.mediturno.reservas.aplicacion.TarificadorLocal;
import cl.losandes.mediturno.reservas.aplicacion.TarificadorPort;
import cl.losandes.mediturno.reservas.aplicacion.ValidadorPrevision;
import cl.losandes.mediturno.reservas.infraestructura.clientes.ValidadorPrevisionPermisivo;
import cl.losandes.mediturno.reservas.infraestructura.mensajeria.PublicadorNulo;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroIdempotenciaMemoria;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroIdempotenciaRedis;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroOfertasMemoria;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroOfertasRedis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Armado de la aplicación.
 *
 * <p>Hay dos formas de levantar ms-reservas y la diferencia está toda aquí.</p>
 *
 * <ul>
 *   <li><b>Perfil por defecto</b> — el del Release 2.1: PostgreSQL, Redis, Kafka y los
 *       otros servicios por HTTP. Es el que se despliega y el que usan las pruebas de
 *       integración.</li>
 *   <li><b>Perfil {@code efimero}</b> — sin infraestructura: H2 en memoria, tarificación
 *       local, sin broker y sin validador externo. Es el que permite que las suites de
 *       las entregas 1 y 2 sigan corriendo tal cual fueron escritas, en segundos y sin
 *       Docker.</li>
 * </ul>
 */
@Configuration
public class ConfiguracionAplicacion {

    @Bean
    public Reloj reloj() {
        return new RelojSistema();
    }

    // ------------------------------------------------------- perfil efimero

    @Bean
    @Profile("efimero")
    public TarificadorPort tarificadorLocal() {
        return new TarificadorLocal();
    }

    @Bean
    @Profile("efimero")
    public ValidadorPrevision validadorPermisivo() {
        return new ValidadorPrevisionPermisivo();
    }

    @Bean
    @Profile("efimero")
    public PublicadorEventos publicadorNulo() {
        return new PublicadorNulo();
    }

    @Bean
    @Profile("efimero")
    public RegistroOfertas registroOfertasMemoria() {
        return new RegistroOfertasMemoria();
    }

    @Bean
    @Profile("efimero")
    public RegistroIdempotencia registroIdempotenciaMemoria() {
        return new RegistroIdempotenciaMemoria();
    }

    // -------------------------------------------------- perfil por defecto

    @Bean
    @Profile("!efimero")
    public RegistroOfertas registroOfertasRedis(StringRedisTemplate redis) {
        return new RegistroOfertasRedis(redis);
    }

    @Bean
    @Profile("!efimero")
    public RegistroIdempotencia registroIdempotenciaRedis(StringRedisTemplate redis) {
        return new RegistroIdempotenciaRedis(redis);
    }
}

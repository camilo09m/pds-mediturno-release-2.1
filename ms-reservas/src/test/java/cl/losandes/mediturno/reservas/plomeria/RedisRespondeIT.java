package cl.losandes.mediturno.reservas.plomeria;

import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;
import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plomería: Redis responde y las dos operaciones de las que depende el sistema se
 * comportan como se espera.
 *
 * <p>La deduplicación depende de que SET NX sea atómico y de que la primera llamada gane.
 * Si esa propiedad no se cumple, todo el tratamiento de mensajes duplicados es humo. Vale
 * la pena comprobarla contra un Redis real y no contra un doble.</p>
 */
class RedisRespondeIT extends BaseDePlomeria {

    @Autowired RegistroIdempotencia idempotencia;
    @Autowired RegistroOfertas ofertas;

    @Test
    @DisplayName("la primera vez que se ve una clave gana, la segunda pierde")
    void deduplicacion() {
        String clave = "plomeria-" + UUID.randomUUID();

        assertThat(idempotencia.registrarSiEsNuevo(clave, Duration.ofMinutes(5))).isTrue();
        assertThat(idempotencia.registrarSiEsNuevo(clave, Duration.ofMinutes(5))).isFalse();

        idempotencia.olvidar(clave);
        assertThat(idempotencia.registrarSiEsNuevo(clave, Duration.ofMinutes(5))).isTrue();
        idempotencia.olvidar(clave);
    }

    @Test
    @DisplayName("una oferta abierta queda vigente y se puede cerrar")
    void ventanaDeOferta() {
        Long id = System.nanoTime();
        LocalDateTime ahora = LocalDateTime.now();

        ofertas.abrir(id, ahora.plusMinutes(15), ahora);
        assertThat(ofertas.vigente(id)).isTrue();
        assertThat(ofertas.vigentes()).contains(id);

        ofertas.cerrar(id);
        assertThat(ofertas.vigente(id)).isFalse();
    }

    @Test
    @DisplayName("una ventana ya vencida no se abre")
    void ventanaVencidaNoSeAbre() {
        Long id = System.nanoTime();
        LocalDateTime ahora = LocalDateTime.now();

        ofertas.abrir(id, ahora.minusMinutes(1), ahora);
        assertThat(ofertas.vigente(id)).isFalse();
    }
}

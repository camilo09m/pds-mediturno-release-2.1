package cl.losandes.mediturno.reservas.infraestructura.clientes;

import cl.losandes.mediturno.reservas.aplicacion.ValidadorPrevision;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente del Validador de Previsión.
 *
 * <p>Respuestas contempladas: 200 afiliado, 404 no afiliado, 429 cuota agotada,
 * 503 servicio caído. El timeout de lectura es de 3 s, seis veces su SLA declarado.</p>
 *
 * <p>Ante indisponibilidad se degrada en vez de rechazar: la reserva sigue adelante sin
 * verificación. Es una decisión de negocio, no técnica — un 2,8 % de indisponibilidad
 * bloquearía una de cada treinta y cinco reservas. La contrapartida es que un paciente
 * con previsión mal declarada puede reservar; la conciliación posterior corrige el
 * copago.</p>
 */
@Component
@Profile("!efimero")
public class ValidadorPrevisionHttp implements ValidadorPrevision {

    private static final Logger log = LoggerFactory.getLogger(ValidadorPrevisionHttp.class);

    private final RestClient cliente;

    public ValidadorPrevisionHttp(@Qualifier("clienteValidadorPrevision") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    @Retry(name = "validadorPrevision")
    @CircuitBreaker(name = "validadorPrevision", fallbackMethod = "sinValidador")
    public Resultado validar(String rut) {
        try {
            Contratos.ValidacionPrevision v = cliente.get()
                    .uri("/prevision/{rut}", rut)
                    .retrieve()
                    .body(Contratos.ValidacionPrevision.class);
            if (v == null) {
                return Resultado.noVerificado();
            }
            return new Resultado(true, v.afiliado(), v.prevision(), v.tramo());
        } catch (HttpClientErrorException.NotFound e) {
            return new Resultado(true, false, null, null);
        }
    }

    @SuppressWarnings("unused")
    private Resultado sinValidador(String rut, Throwable causa) {
        log.warn("Validador de Previsión no disponible para {}: {}. "
                + "La reserva continúa sin verificación previsional.", rut, causa.toString());
        return Resultado.noVerificado();
    }
}

package cl.losandes.mediturno.reservas.infraestructura.mensajeria;

import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;
import cl.losandes.mediturno.reservas.aplicacion.ReservaService;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ConfirmacionPago;
import cl.losandes.mediturno.reservas.dominio.ReglaNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Consume las confirmaciones de pago que publica ms-pagos.
 *
 * <p>Webpay puede enviar el mismo webhook más de una vez, y Kafka entrega al menos una
 * vez: entre ambas cosas, recibir dos veces la misma confirmación es lo normal, no la
 * excepción. La deduplicación se hace por {@code confirmacionId} contra Redis, con una
 * ventana de 24 horas.</p>
 */
@Component
@Profile("!efimero")
public class ConsumidorConfirmacionesPago {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorConfirmacionesPago.class);
    private static final Duration VENTANA_DEDUPLICACION = Duration.ofHours(24);

    private final ReservaService reservas;
    private final RegistroIdempotencia procesados;
    private final ObjectMapper json;

    public ConsumidorConfirmacionesPago(ReservaService reservas, RegistroIdempotencia procesados,
                                        ObjectMapper json) {
        this.reservas = reservas;
        this.procesados = procesados;
        this.json = json;
    }

    @KafkaListener(
            topics = "#{T(cl.losandes.mediturno.reservas.aplicacion.eventos.Topics).PAGOS_CONFIRMACIONES}",
            groupId = "${mediturno.kafka.grupo-consumidor:ms-reservas}")
    public void alRecibir(String carga) throws JsonProcessingException {
        // Una carga útil ilegible no mejora reintentando: el manejador de errores la
        // deriva de inmediato a la cola de mensajes muertos.
        ConfirmacionPago pago = json.readValue(carga, ConfirmacionPago.class);

        if (pago.confirmacionId() == null || pago.idReserva() == null) {
            throw new IllegalArgumentException(
                    "Confirmación de pago sin confirmacionId o sin idReserva: " + carga);
        }
        if (!procesados.registrarSiEsNuevo(pago.confirmacionId(), VENTANA_DEDUPLICACION)) {
            log.info("Confirmación de pago {} ya procesada; se descarta la repetición.",
                    pago.confirmacionId());
            return;
        }
        if (!pago.autorizado()) {
            log.info("Pago {} de la reserva {} no autorizado ({}); no se confirma la reserva.",
                    pago.ordenCompra(), pago.idReserva(), pago.estado());
            return;
        }

        try {
            reservas.confirmar(pago.idReserva());
            log.info("Reserva {} confirmada por el pago {}.", pago.idReserva(), pago.ordenCompra());
        } catch (ReglaNegocioException e) {
            if ("RES-404".equals(e.codigo())) {
                // El webhook llegó antes que la reserva existiera. Se libera la marca y se
                // deja que el mensaje reintente; si tras los reintentos la reserva sigue sin
                // aparecer, termina en la cola de mensajes muertos, que es donde debe quedar.
                procesados.olvidar(pago.confirmacionId());
                throw e;
            }
            // Caso conocido: el pago se autorizó sobre una reserva que ya no admite
            // confirmación, típicamente porque fue anulada. Hoy el dinero queda cobrado y
            // la reserva anulada, sin compensación ni reverso. Está registrado como deuda
            // técnica del Release 2.1.
            log.warn("Pago {} autorizado sobre la reserva {}, que no admite confirmación ({}). "
                            + "No hay reverso automático: requiere revisión manual.",
                    pago.ordenCompra(), pago.idReserva(), e.codigo());
        }
    }
}

package cl.losandes.mediturno.pagos.mensajeria;

import cl.losandes.mediturno.pagos.api.WebhookWebpayRequest;
import cl.losandes.mediturno.pagos.eventos.ConfirmacionPago;
import cl.losandes.mediturno.pagos.eventos.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Traduce el webhook de Webpay a un evento interno y lo publica.
 *
 * <p>Deduplica por orden de compra y código de autorización, porque Webpay reenvía el
 * mismo webhook cuando no recibe respuesta a tiempo. Aun así el consumidor tiene que
 * deduplicar por su cuenta: Kafka entrega al menos una vez, de modo que un mensaje
 * publicado una sola vez puede llegar dos veces.</p>
 */
@Component
public class PublicadorPagos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorPagos.class);
    private static final String PREFIJO = "mediturno:webhook:";
    private static final Duration VENTANA_DEDUPLICACION = Duration.ofHours(24);
    private static final String PREFIJO_ORDEN = "MT-";

    private final KafkaTemplate<String, String> plantilla;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public PublicadorPagos(KafkaTemplate<String, String> plantilla, StringRedisTemplate redis,
                           ObjectMapper json) {
        this.plantilla = plantilla;
        this.redis = redis;
        this.json = json;
    }

    /** @return false si este webhook ya se había recibido y publicado. */
    public boolean publicar(WebhookWebpayRequest req) {
        String huella = req.ordenCompra() + ":" + req.codigoAutorizacion();
        Boolean primero = redis.opsForValue()
                .setIfAbsent(PREFIJO + huella, "1", VENTANA_DEDUPLICACION);
        if (!Boolean.TRUE.equals(primero)) {
            return false;
        }

        ConfirmacionPago confirmacion = new ConfirmacionPago(
                UUID.randomUUID().toString(),
                ConfirmacionPago.VERSION_ACTUAL,
                idReservaDe(req.ordenCompra()),
                req.ordenCompra(),
                traducirEstado(req.estado()),
                req.monto(),
                req.fechaTransaccion() == null ? LocalDateTime.now() : req.fechaTransaccion());

        try {
            plantilla.send(Topics.PAGOS_CONFIRMACIONES,
                    req.ordenCompra(), json.writeValueAsString(confirmacion));
        } catch (JsonProcessingException e) {
            redis.delete(PREFIJO + huella);
            throw new IllegalStateException("No se pudo serializar la confirmación de pago.", e);
        }
        return true;
    }

    /**
     * La orden de compra tiene la forma {@code MT-000123}, donde el número es el id de la
     * reserva. Si no calza con ese formato se publica igual con id nulo: el consumidor lo
     * derivará a la cola de mensajes muertos, que es donde debe quedar una orden que no
     * sabemos a qué reserva corresponde.
     */
    private Long idReservaDe(String ordenCompra) {
        if (ordenCompra == null || !ordenCompra.startsWith(PREFIJO_ORDEN)) {
            log.warn("Orden de compra con formato inesperado: {}", ordenCompra);
            return null;
        }
        try {
            return Long.parseLong(ordenCompra.substring(PREFIJO_ORDEN.length()));
        } catch (NumberFormatException e) {
            log.warn("Orden de compra sin número de reserva utilizable: {}", ordenCompra);
            return null;
        }
    }

    private String traducirEstado(String estadoWebpay) {
        return switch (estadoWebpay == null ? "" : estadoWebpay.toUpperCase()) {
            case "AUTHORIZED" -> "AUTORIZADO";
            case "FAILED", "REJECTED" -> "RECHAZADO";
            case "REVERSED", "NULLIFIED" -> "REVERTIDO";
            default -> "DESCONOCIDO";
        };
    }
}

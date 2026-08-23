package cl.losandes.mediturno.notificaciones.mensajeria;

import cl.losandes.mediturno.notificaciones.eventos.SolicitudNotificacion;
import cl.losandes.mediturno.notificaciones.proveedor.ProveedorMensajeria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Despacha los avisos que ms-reservas pide de forma explícita. */
@Component
public class ConsumidorSolicitudes {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorSolicitudes.class);
    private static final Duration VENTANA_DEDUPLICACION = Duration.ofHours(24);

    private final ProveedorMensajeria proveedor;
    private final RegistroIdempotencia procesados;
    private final ObjectMapper json;

    public ConsumidorSolicitudes(ProveedorMensajeria proveedor, RegistroIdempotencia procesados,
                                 ObjectMapper json) {
        this.proveedor = proveedor;
        this.procesados = procesados;
        this.json = json;
    }

    @KafkaListener(
            topics = "#{T(cl.losandes.mediturno.notificaciones.eventos.Topics).NOTIFICACIONES_SOLICITUDES}",
            groupId = "${mediturno.kafka.grupo-consumidor:ms-notificaciones}")
    public void alRecibir(String carga) throws JsonProcessingException {
        SolicitudNotificacion s = json.readValue(carga, SolicitudNotificacion.class);

        if (s.solicitudId() == null || s.rutPaciente() == null) {
            throw new IllegalArgumentException("Solicitud sin solicitudId o sin destinatario: " + carga);
        }
        if (!procesados.registrarSiEsNuevo(s.solicitudId(), VENTANA_DEDUPLICACION)) {
            log.info("Solicitud {} ya despachada; se descarta la repetición.", s.solicitudId());
            return;
        }
        try {
            proveedor.enviar(s.canal(), s.rutPaciente(), s.cuerpo());
            log.info("Aviso {} enviado a {}.", s.plantilla(), s.rutPaciente());
        } catch (RuntimeException e) {
            // El envío falló: se libera la marca para que el reintento no la encuentre
            // tomada y descarte el mensaje sin haberlo enviado nunca.
            procesados.olvidar(s.solicitudId());
            throw e;
        }
    }
}

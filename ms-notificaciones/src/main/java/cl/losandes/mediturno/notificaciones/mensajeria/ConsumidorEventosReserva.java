package cl.losandes.mediturno.notificaciones.mensajeria;

import cl.losandes.mediturno.notificaciones.eventos.ReservaEvento;
import cl.losandes.mediturno.notificaciones.proveedor.ProveedorMensajeria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Avisa al paciente los cambios de estado que le importan.
 *
 * <p>No todos los eventos generan aviso: iniciar y finalizar una atención son hechos
 * internos. Un evento sin aviso no es un error, se descarta en silencio.</p>
 */
@Component
public class ConsumidorEventosReserva {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosReserva.class);
    private static final Duration VENTANA_DEDUPLICACION = Duration.ofHours(24);
    private static final Set<String> AVISABLES = Set.of("CREADA", "CONFIRMADA", "ANULADA", "MODIFICADA");

    private final ProveedorMensajeria proveedor;
    private final RegistroIdempotencia procesados;
    private final ObjectMapper json;

    public ConsumidorEventosReserva(ProveedorMensajeria proveedor, RegistroIdempotencia procesados,
                                    ObjectMapper json) {
        this.proveedor = proveedor;
        this.procesados = procesados;
        this.json = json;
    }

    @KafkaListener(
            topics = "#{T(cl.losandes.mediturno.notificaciones.eventos.Topics).RESERVAS_EVENTOS}",
            groupId = "${mediturno.kafka.grupo-consumidor:ms-notificaciones}")
    public void alRecibir(String carga) throws JsonProcessingException {
        ReservaEvento e = json.readValue(carga, ReservaEvento.class);

        if (e.eventoId() == null || e.tipo() == null) {
            throw new IllegalArgumentException("Evento sin eventoId o sin tipo: " + carga);
        }
        if (!AVISABLES.contains(e.tipo())) {
            return;
        }
        if (!procesados.registrarSiEsNuevo(e.eventoId(), VENTANA_DEDUPLICACION)) {
            log.info("Evento {} ya avisado; se descarta la repetición.", e.eventoId());
            return;
        }
        try {
            proveedor.enviar("SMS", e.rutPaciente(), redactar(e));
            log.info("Aviso de {} enviado a {} por la reserva {}.",
                    e.tipo(), e.rutPaciente(), e.idReserva());
        } catch (RuntimeException ex) {
            procesados.olvidar(e.eventoId());
            throw ex;
        }
    }

    private String redactar(ReservaEvento e) {
        return switch (e.tipo()) {
            case "CREADA" -> "Su hora de " + e.especialidad() + " quedó tomada para el "
                    + e.fechaHoraAtencion() + ".";
            case "CONFIRMADA" -> "Su hora de " + e.especialidad() + " del "
                    + e.fechaHoraAtencion() + " está confirmada.";
            case "ANULADA" -> "Su hora de " + e.especialidad() + " del "
                    + e.fechaHoraAtencion() + " fue anulada.";
            case "MODIFICADA" -> "Su hora de " + e.especialidad() + " quedó reprogramada para el "
                    + e.fechaHoraAtencion() + ".";
            default -> "Hubo un cambio en su hora de " + e.especialidad() + ".";
        };
    }
}

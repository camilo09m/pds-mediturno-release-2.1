package cl.losandes.mediturno.reservas.infraestructura.mensajeria;

import cl.losandes.mediturno.reservas.aplicacion.PublicadorEventos;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.SolicitudNotificacion;
import cl.losandes.mediturno.reservas.aplicacion.eventos.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!efimero")
public class PublicadorKafka implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorKafka.class);

    private final KafkaTemplate<String, String> plantilla;
    private final ObjectMapper json;

    public PublicadorKafka(KafkaTemplate<String, String> plantilla, ObjectMapper json) {
        this.plantilla = plantilla;
        this.json = json;
    }

    @Override
    public void publicar(ReservaEvento evento) {
        enviar(Topics.RESERVAS_EVENTOS, evento.clave(), evento);
    }

    @Override
    public void publicar(SolicitudNotificacion solicitud) {
        enviar(Topics.NOTIFICACIONES_SOLICITUDES, solicitud.rutPaciente(), solicitud);
    }

    private void enviar(String topic, String clave, Object carga) {
        try {
            plantilla.send(topic, clave, json.writeValueAsString(carga));
        } catch (JsonProcessingException e) {
            // Serializar un evento propio no debería fallar nunca. Si falla, el evento se
            // pierde: se registra con el detalle suficiente para reconstruirlo a mano.
            log.error("No se pudo serializar el evento {} para el topic {}: {}",
                    carga, topic, e.getMessage());
        }
    }
}

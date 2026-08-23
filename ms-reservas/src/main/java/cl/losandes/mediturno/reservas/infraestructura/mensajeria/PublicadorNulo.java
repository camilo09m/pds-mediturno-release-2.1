package cl.losandes.mediturno.reservas.infraestructura.mensajeria;

import cl.losandes.mediturno.reservas.aplicacion.PublicadorEventos;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.SolicitudNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publicador que no publica, para el perfil {@code efimero}. Deja rastro en el log para
 * que se note que el evento se emitió y no salió a ninguna parte.
 */
public class PublicadorNulo implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorNulo.class);

    @Override
    public void publicar(ReservaEvento evento) {
        log.debug("[efimero] evento no publicado: {} reserva {}", evento.tipo(), evento.idReserva());
    }

    @Override
    public void publicar(SolicitudNotificacion solicitud) {
        log.debug("[efimero] notificación no publicada: {} a {}",
                solicitud.plantilla(), solicitud.rutPaciente());
    }
}
